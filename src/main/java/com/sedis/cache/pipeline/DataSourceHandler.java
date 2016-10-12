package com.sedis.cache.pipeline;

import com.sedis.common.util.JsonUtils;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public class DataSourceHandler extends AbstractCacheHandler {

    private static Logger logger = Logger.getLogger(DataSourceHandler.class);

    private MethodInvocation invocation;

    public DataSourceHandler() {
    }

    public DataSourceHandler(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    public void setInvocation(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    @Override
    public <V> V forwardHandle(CacheHandlerContext context) {
        if (invocation == null) {
            return null;
        }
        final ReentrantLock lock = context.getLock() == null ? RedisCacheHandler.getLock(context.getKey()) : context.getLock();
        lock.lock();
        try {
            return (V) invocation.proceed();
        } catch (Throwable t) {
            logger.error(MessageFormat.format("DataSourceHandlerError, the context is {0}", JsonUtils.beanToJson(context)), t);
        } finally {
            // if datasource level, unlock only once
            // if datsource and redis, unlock once here, redis_reverse unlock twice
            lock.unlock();
        }
        return null;
    }

    @Override
    public void reverseHandle(CacheHandlerContext context) {
        // do nothing
    }

}
