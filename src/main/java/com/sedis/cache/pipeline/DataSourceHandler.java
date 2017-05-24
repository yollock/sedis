package com.sedis.cache.pipeline;

import com.sedis.cache.common.SedisConst;
import com.sedis.cache.spring.CacheInterceptor;
import com.sedis.util.JsonUtil;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

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
        logger.debug("DataSourceHandler.handle context: " + context);
        if ((context.getHandlerFlag() & CacheHandlerContext.DATASOURCE_HANDLER) == 0) {
            return null;
        }
        try {
            switch (context.getCacheAttribute().getType()) {
                case SedisConst.CACHE:
                case SedisConst.CACHE_EXPIRE:
                    if (invocation == null) {
                        logger.info("MethodInvocation is null, context is: " + JsonUtil.beanToJson(context));
                        return null;
                    }
                    return (V) invocation.proceed();
                case SedisConst.CACHE_UPDATE:
                    // 更新数据, 同时构建任务执行删除和查询操作, 实现缓存更新
                    V result = (V) invocation.proceed();
                    submitTask(context);
                    return result;
                default:
                    return null;
            }
        } catch (Throwable e) {
            logger.error("DataSourceHandlerError, the context is " + JsonUtil.beanToJson(context), e);
        }
        return null;
    }

    private void submitTask(CacheHandlerContext context) {
        final CacheInterceptor interceptor = context.getInterceptor();
        if (interceptor == null) {
            logger.warn("CacheInterceptor is null, will not submit a CacheTask: " + JsonUtil.beanToJson(context));
            return;
        }

        List<Integer> types = new ArrayList<Integer>(2);
        types.add(SedisConst.CACHE_EXPIRE);
        types.add(SedisConst.CACHE);
        interceptor.submit(interceptor, context.getCacheAttribute(), context.getKey(), types);
    }


}
