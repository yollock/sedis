package com.sedis.cache.spring;

import com.sedis.cache.adapter.ClientAdapter;
import com.sedis.cache.common.SedisConst;
import com.sedis.cache.pipeline.CacheHandlerContext;
import com.sedis.cache.pipeline.CachePipeline;
import com.sedis.cache.pipeline.DefaultCachePipeline;
import com.sedis.util.CollectionUtil;
import com.sedis.util.JsonUtil;
import com.sedis.util.LogUtil;
import com.sedis.util.StringUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * advice, from Advisor.getAdvice()
 */
public class CacheInterceptor implements MethodInterceptor, ApplicationContextAware, InitializingBean, DisposableBean, Serializable {

    private int memoryCount;
    private ClientAdapter sedisClient;
    private CacheAttributeSource cacheAttributeSource;

    // Scavenger params
    private int lockCount;
    private long maxPeriod;
    private long delay;

    public static ApplicationContext applicationContext;

    private final ConcurrentMap<CacheAttribute, CachePipeline> pipelines = new ConcurrentHashMap<CacheAttribute, CachePipeline>();
    // MethodInvocation is stateful with params, so, call with correct params
    // MethodInvocation有状态,因为参数可能不一样, 所以, 使用时必须使用正确的参数
    private final ConcurrentMap<CacheAttribute, MethodInvocation> invocations = new ConcurrentHashMap<CacheAttribute, MethodInvocation>();
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    public CacheInterceptor() {
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);
        final CacheAttribute cacheAttr = this.cacheAttributeSource.getCacheAttribute(invocation.getMethod(), targetClass);
        // if cache annotation is null, fail fast with invocation.proceed()
        // 如果Cache注解信息为空,直接调用目标方法,返回值
        if (cacheAttr == null) {
            return invocation.proceed();
        }
        // 从CacheAttribute获取需要参与的handler的值,如果不合法,返回 0
        // if handlerFlag forbidden, fail fast with invocation.proceed()
        int handlerFlag = CacheAttrUtil.getHandlerFlag(cacheAttr);
        if (handlerFlag == CacheHandlerContext.BOTTOM_HANDLER) {
            return invocation.proceed();
        }

        // 保存pipeline和invocation
        // save pipeline and invocation
        CachePipeline pipeline = pipelines.get(cacheAttr);
        if (pipeline == null) {
            pipelines.put(cacheAttr, new DefaultCachePipeline(this));
            pipeline = pipelines.get(cacheAttr);
        }
        invocations.putIfAbsent(cacheAttr, invocation);

        // uniqueKey与CacheAttribute的key不一样, key是模版, uniqueKey则是根据模版key和具体的参数构建而成
        // uniqueKey and CacheAttribute.key is different, key is a param model,
        // uniqueKey created by key and real params from invocation
        String uniqueKey = CacheAttrUtil.getUniqueKey(cacheAttr, invocation);
        return pipeline.handle(new CacheHandlerContext(this, CacheAttrUtil.copy(cacheAttr), invocation, uniqueKey, handlerFlag));
    }

    private class CacheTask implements Runnable {
        private CacheInterceptor interceptor;
        private CacheAttribute cacheAttr;
        private String uniqueKey;
        private List<Integer> types; // CacheAttribute.type list, a simple chain

        CacheTask(CacheInterceptor interceptor, CacheAttribute cacheAttr, String uniqueKey, List<Integer> types) {
            this.interceptor = interceptor;
            this.cacheAttr = cacheAttr;
            this.uniqueKey = uniqueKey;
            this.types = types;
        }

        /**
         * 更新缓存, 触发删除和查询::
         * 删除可以执行memory和redis, 但不能执行datasource;
         * 查询都可以执行
         * --------------------
         *
         * @CacheUpdate trigger @CacheExpire and @Cache
         * at this time,
         * @CacheExpire can work with MemoryCacheHandler and RedisCacheHandler, but DatasourceHandler
         * @Cache no limit
         */
        @Override
        public void run() {
            if (cacheAttr == null || StringUtil.isEmpty(uniqueKey) || CollectionUtil.isEmpty(types)) {
                return;
            }
            try {
                for (Integer type : types) {
                    final CacheAttribute targetCacheAttr = CacheAttrUtil.copy(cacheAttr).setType(type);
                    CachePipeline pipeline;
                    MethodInvocation invocation;
                    if (type == SedisConst.CACHE_EXPIRE) {
                        targetCacheAttr.setDataSourceEnable(false); // 不执行数据源删除操作, do not delete datasource
                        pipeline = optimalPipeline(CacheAttrUtil.copy(targetCacheAttr));
                        invocation = DefaultMethodInvocation.instance();
                    } else {
                        pipeline = interceptor.getPipelines().get(targetCacheAttr);
                        invocation = interceptor.getInvocations().get(targetCacheAttr);
                    }

                    if (pipeline == null || invocation == null) {
                        LogUtil.warn("pipeline or invocation is null, CacheTask will not work, CacheAttribute is " + targetCacheAttr);
                        return;
                    }

                    if (type != SedisConst.CACHE_EXPIRE) {
                        ReflectiveMethodInvocation refInvocation;
                        if (invocation instanceof ReflectiveMethodInvocation) {
                            refInvocation = (ReflectiveMethodInvocation) invocation;
                        } else {
                            LogUtil.warn("invocation not instanceof ReflectiveMethodInvocation, real type is " + invocation.getClass().getName());
                            return;
                        }
                        if (!transferParam(refInvocation, uniqueKey)) {
                            return;
                        }
                    }
                    pipeline.handle(new CacheHandlerContext(interceptor, targetCacheAttr, invocation, uniqueKey, CacheAttrUtil.getHandlerFlag(targetCacheAttr)));
                }
            } catch (Throwable e) {
                LogUtil.warn("CacheTask error", e);
            }
        }

        @Override
        public String toString() {
            return "CacheTask{" + "cacheAttr=" + cacheAttr + ", uniqueKey='" + uniqueKey + '\'' + ", types=" + JsonUtil.beanToJson(types) + '}';
        }
    }

    private CachePipeline optimalPipeline(CacheAttribute cacheAttr) {
        CachePipeline optimalPipeline;
        // @Cache fisrt
        cacheAttr.setType(SedisConst.CACHE);
        optimalPipeline = pipelines.get(cacheAttr);
        if (optimalPipeline != null) {
            return optimalPipeline;
        }
        // @CacheUpdate second
        cacheAttr.setType(SedisConst.CACHE_UPDATE);
        optimalPipeline = pipelines.get(cacheAttr);
        if (optimalPipeline != null) {
            return optimalPipeline;
        }
        // @CacheExpire third
        cacheAttr.setType(SedisConst.CACHE_EXPIRE);
        optimalPipeline = pipelines.get(cacheAttr);
        if (optimalPipeline != null) {
            return optimalPipeline;
        }
        return null;
    }

    /**
     * Default MethodInvocation avoid to delete data when type is CACHE_EXPIRE
     */
    private static class DefaultMethodInvocation extends ReflectiveMethodInvocation {

        protected DefaultMethodInvocation(Object proxy, //
                                          Object target, //
                                          Method method, //
                                          Object[] arguments, //
                                          Class<?> targetClass, //
                                          List<Object> interceptorsAndDynamicMethodMatchers //
        ) {
            super(proxy, target, method, arguments, targetClass, interceptorsAndDynamicMethodMatchers);
        }

        public static DefaultMethodInvocation instance() {
            return new DefaultMethodInvocation(null, null, null, null, null, null);
        }
    }

    public void submit(CacheInterceptor interceptor, CacheAttribute cacheAttribute, String key, List<Integer> types) {
        try {
            final CacheTask task = new CacheTask(interceptor, cacheAttribute, key, types);
            executor.submit(task);
            LogUtil.debug("CacheInterceptor.submit CacheTask: " + task);
        } catch (Throwable e) {
            LogUtil.warn("CacheInterceptor.submit error", e);
        }
    }

    private boolean transferParam(ReflectiveMethodInvocation refInvocation, String uniqueKey) {
        Class[] parameterTypes = refInvocation.getMethod().getParameterTypes();
        String[] newArguments = CacheAttrUtil.params(uniqueKey);
        if (parameterTypes.length != newArguments.length) {
            LogUtil.warn("target params length from uniqueKey and invocation params length not equal");
            return false;
        }
        Object[] arguments = new Object[parameterTypes.length];
        for (int i = 0, len = parameterTypes.length; i < len; i++) {
            final String parameterTypeName = parameterTypes[i].getName();
            if (String.class.getName().equals(parameterTypeName)) {
                arguments[i] = newArguments[i];
            } else if (Integer.class.getName().equals(parameterTypeName)) {
                arguments[i] = Integer.parseInt(newArguments[i]);
            } else if (Long.class.getName().equals(parameterTypeName)) {
                arguments[i] = Long.parseLong(newArguments[i]);
            } else if (Double.class.getName().equals(parameterTypeName)) {
                arguments[i] = Double.parseDouble(newArguments[i]);
            } else if (Float.class.getName().equals(parameterTypeName)) {
                arguments[i] = Float.parseFloat(newArguments[i]);
            } else if (Boolean.class.getName().equals(parameterTypeName)) {
                arguments[i] = Boolean.parseBoolean(newArguments[i]);
            } else {
                LogUtil.warn("the " + i + "th parameterType[" + parameterTypeName + "] can not convert, uniqueKey is" + uniqueKey);
                return false;
            }
        }
        refInvocation.setArguments(arguments);
        return true;
    }

    // ioc

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CacheInterceptor.applicationContext = applicationContext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        final Map<Object, CacheAttribute> cacheAttrMap = ((AnnotationCacheAttributeSource) cacheAttributeSource).getAttributeCache();
        for (CacheAttribute cacheAttribute : cacheAttrMap.values()) {
            if (StringUtil.isEmpty(cacheAttribute.getKey())) {
                continue;
            }
            pipelines.put(cacheAttribute, new DefaultCachePipeline(this));
        }
    }

    @Override
    public void destroy() throws Exception {
        executor.shutdown();
        for (CachePipeline pipeline : pipelines.values()) {
            pipeline.destroy();
        }
        invocations.clear();
    }

    // setter

    public void setSedisClient(ClientAdapter sedisClient) {
        this.sedisClient = sedisClient;
    }

    public void setCacheAttributeSource(CacheAttributeSource cacheAttributeSource) {
        this.cacheAttributeSource = cacheAttributeSource;
    }

    public void setMemoryCount(int memoryCount) {
        this.memoryCount = memoryCount;
    }

    public void setLockCount(int lockCount) {
        this.lockCount = lockCount;
    }

    public void setMaxPeriod(long maxPeriod) {
        this.maxPeriod = maxPeriod;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public int getMemoryCount() {
        return memoryCount;
    }

    public ConcurrentMap<CacheAttribute, CachePipeline> getPipelines() {
        return pipelines;
    }

    public ConcurrentMap<CacheAttribute, MethodInvocation> getInvocations() {
        return invocations;
    }

    public ClientAdapter getSedisClient() {
        return sedisClient;
    }

    public int getLockCount() {
        return lockCount;
    }

    public long getMaxPeriod() {
        return maxPeriod;
    }

    public long getDelay() {
        return delay;
    }

    public CacheAttributeSource getCacheAttributeSource() {
        return cacheAttributeSource;
    }
}
