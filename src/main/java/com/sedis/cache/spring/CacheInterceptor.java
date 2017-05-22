package com.sedis.cache.spring;

import com.sedis.cache.pipeline.CacheHandlerContext;
import com.sedis.cache.pipeline.CachePipeline;
import com.sedis.cache.pipeline.DefaultCachePipeline;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import redis.clients.jedis.ShardedJedisPool;

import java.io.Serializable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 执行增强的拦截器,从Advisor调用getAdvice()获取
 */
public class CacheInterceptor implements MethodInterceptor, ApplicationContextAware, Serializable {

    private int memoryCount;
    private ShardedJedisPool sedisClient;
    private CacheAttributeSource cacheAttributeSource;

    // 清道夫属性
    private int lockCount;
    private long maxPeriod;
    private long delay;

    public static ApplicationContext applicationContext;

    private final ConcurrentMap<CacheAttribute, CachePipeline> pipelines = new ConcurrentHashMap<CacheAttribute, CachePipeline>();

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
        int handlerFlag = CacheAttributeUtils.getHandlerFlag(cacheAttr);
        String key = CacheAttributeUtils.getKey(cacheAttr, invocation);

        if (handlerFlag == CacheHandlerContext.BOTTOM_HANDLER) {
            return invocation.proceed();
        }

        CachePipeline pipeline = pipelines.get(cacheAttr);
        if (pipeline == null) {
            pipelines.put(cacheAttr, new DefaultCachePipeline(this));
            pipeline = pipelines.get(cacheAttr);
        }
        return pipeline.handle(new CacheHandlerContext(cacheAttr, invocation, key, handlerFlag));
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
