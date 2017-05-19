package com.sedis.cache.pipeline;

import com.sedis.util.JsonUtil;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;

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
            logger.warn("目标方法为null,获取原始数据失败 " + JsonUtil.beanToJson(context));
            return null;
        }
        try {
            return (V) invocation.proceed();
        } catch (Throwable e) {
            logger.error("DataSourceHandlerError, the context is " + JsonUtil.beanToJson(context), e);
        }
        return null;
    }


}
