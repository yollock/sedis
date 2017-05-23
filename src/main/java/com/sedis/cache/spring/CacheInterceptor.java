package com.sedis.cache.spring;

import com.sedis.cache.common.SedisConst;
import com.sedis.cache.pipeline.CacheHandlerContext;
import com.sedis.cache.pipeline.CachePipeline;
import com.sedis.cache.pipeline.DefaultCachePipeline;
import com.sedis.util.CollectionUtil;
import com.sedis.util.StringUtil;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;
import org.springframework.aop.framework.ReflectiveMethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import redis.clients.jedis.ShardedJedisPool;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 执行增强的拦截器,从Advisor调用getAdvice()获取
 */
public class CacheInterceptor implements MethodInterceptor, ApplicationContextAware, Serializable {

    private static Logger logger = Logger.getLogger(CacheInterceptor.class);

    private int memoryCount;
    private ShardedJedisPool sedisClient;
    private CacheAttributeSource cacheAttributeSource;

    // 清道夫属性
    private int lockCount;
    private long maxPeriod;
    private long delay;

    public static ApplicationContext applicationContext;

    private final ConcurrentMap<CacheAttribute, CachePipeline> pipelines = new ConcurrentHashMap<CacheAttribute, CachePipeline>();
    // MethodInvocation有状态,因为参数可能不一样, 所以, 使用时必须使用正确的参数
    private final ConcurrentMap<CacheAttribute, MethodInvocation> invocations = new ConcurrentHashMap<CacheAttribute, MethodInvocation>();
    private final ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 2);

    public CacheInterceptor() {
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        CacheInterceptor.applicationContext = applicationContext;
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);
        final CacheAttribute cacheAttr = this.cacheAttributeSource.getCacheAttribute(invocation.getMethod(), targetClass);
        // 如果Cache注解信息为空,直接调用目标方法,返回值
        if (cacheAttr == null) {
            return invocation.proceed();
        }
        // 从CacheAttribute获取需要参与的handler的值,如果不合法,返回 0
        int handlerFlag = CacheAttrUtil.getHandlerFlag(cacheAttr);
        if (handlerFlag == CacheHandlerContext.BOTTOM_HANDLER) {
            return invocation.proceed();
        }

        // 保存pipeline和invocation
        CachePipeline pipeline = pipelines.get(cacheAttr);
        if (pipeline == null) {
            pipelines.put(cacheAttr, new DefaultCachePipeline(this));
            pipeline = pipelines.get(cacheAttr);
        }
        invocations.putIfAbsent(cacheAttr, invocation);

        // uniqueKey与CacheAttribute的key不一样,
        // key是模版, uniqueKey则是根据模版key和具体的参数构建而成
        String uniqueKey = CacheAttrUtil.getUniqueKey(cacheAttr, invocation);
        return pipeline.handle(new CacheHandlerContext(this, CacheAttrUtil.copy(cacheAttr), invocation, uniqueKey, handlerFlag));
    }

    private static class CacheTask implements Runnable {
        private CacheInterceptor interceptor;
        private CacheAttribute cacheAttr;
        private String uniqueKey; // 缓存中的唯一key
        private List<Integer> types; // CacheAttribute的type集合, 为0则是空, 简单的执行链

        public CacheTask(CacheInterceptor interceptor, CacheAttribute cacheAttr, String uniqueKey, List<Integer> types) {
            this.interceptor = interceptor;
            this.cacheAttr = cacheAttr;
            this.uniqueKey = uniqueKey;
            this.types = types;
        }

        /**
         * 更新缓存, 触发删除和查询::
         * 删除可以执行memory和redis, 但不能执行datasource;
         * 查询都可以执行
         */
        @Override
        public void run() {
            if (cacheAttr == null || StringUtil.isEmpty(uniqueKey) || CollectionUtil.isEmpty(types)) {
                return;
            }
            for (Integer type : types) {
                final CacheAttribute targetCacheAttr = CacheAttrUtil.copy(cacheAttr).setType(type);
                if (type == SedisConst.CACHE_EXPIRE) {
                    targetCacheAttr.setDataSourceEnable(false); // 不执行数据源删除操作
                }
                final CachePipeline pipeline = interceptor.getPipelines().get(targetCacheAttr);
                final MethodInvocation invocation = interceptor.getInvocations().get(targetCacheAttr);
                ReflectiveMethodInvocation refInvocation;
                if (invocation instanceof ReflectiveMethodInvocation) {
                    refInvocation = (ReflectiveMethodInvocation) invocation;
                } else {
                    logger.warn("invocation not instanceof ReflectiveMethodInvocation, real type is " + invocation.getClass().getName());
                    return;
                }
                Object[] arguments = refInvocation.getArguments();
                // 更新参数,
                pipeline.handle(new CacheHandlerContext(interceptor, targetCacheAttr, invocation, uniqueKey, CacheAttrUtil.getHandlerFlag(targetCacheAttr)));
            }
        }
    }

    public void submit(CacheInterceptor interceptor, CacheAttribute cacheAttribute, String key, List<Integer> types) {
        try {
            executor.submit(new CacheTask(interceptor, cacheAttribute, key, types));
        } catch (Throwable e) {
            logger.warn("CacheInterceptor.submit error", e);
        }
    }

    // setter

    public void setSedisClient(ShardedJedisPool sedisClient) {
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

    public ShardedJedisPool getSedisClient() {
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
