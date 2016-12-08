package com.sedis.cache.spring;

import com.sedis.cache.pipeline.AbstractCacheHandler;
import com.sedis.cache.pipeline.CacheHandlerContext;
import com.sedis.cache.pipeline.CachePipeline;
import com.sedis.cache.pipeline.DefaultCachePipeline;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.support.AopUtils;
import redis.clients.jedis.ShardedJedisPool;

import java.io.Serializable;
import java.util.concurrent.TimeUnit;

/**
 * 执行增强的拦截器,从Advisor调用getAdvice()获取
 */
public class CacheInterceptor implements MethodInterceptor, Serializable {

    private int memoryCount;
    private ShardedJedisPool sedisClient;
    private CacheAttributeSource cacheAttributeSource;

    // 清道夫属性
    private int lockCount;
    private long maxPeriod;
    private long delay;

    private CachePipeline pipeline = null;

    public CacheInterceptor() {
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        if (pipeline == null) {
            synchronized (this) {
                if (pipeline == null) {
                    pipeline = new DefaultCachePipeline(sedisClient, memoryCount);
                    AbstractCacheHandler.servicer.schedule( //
                            new AbstractCacheHandler.ScavengeWorker(lockCount, maxPeriod), delay, TimeUnit.MILLISECONDS //
                    );
                }
            }
        }
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
}
