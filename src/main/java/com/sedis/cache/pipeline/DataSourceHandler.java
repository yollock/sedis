package com.sedis.cache.pipeline;

import com.sedis.util.JsonUtils;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;

import java.text.MessageFormat;
import java.util.concurrent.locks.ReentrantLock;

/**
 *
 */
public class DataSourceHandler implements CacheHandler {

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
    public <V> V handle(CacheHandlerContext context) {
        if (invocation == null) {
            logger.warn("目标方法为null,获取原始数据失败 " + JsonUtils.beanToJson(context));
            return null;
        }
        try {
            return (V) invocation.proceed();
        } catch (Throwable t) {
            logger.error("DataSourceHandlerError, the context is " + JsonUtils.beanToJson(context), t);
        }
        return null;
    }


}
