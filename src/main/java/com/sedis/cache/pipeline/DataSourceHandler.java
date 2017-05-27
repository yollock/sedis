package com.sedis.cache.pipeline;

import com.sedis.cache.common.SedisConst;
import com.sedis.cache.spring.CacheInterceptor;
import com.sedis.util.JsonUtil;
import com.sedis.util.LogUtil;
import org.aopalliance.intercept.MethodInvocation;

import java.util.ArrayList;
import java.util.List;

public class DataSourceHandler extends AbstractCacheHandler {

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
        LogUtil.debug("DataSourceHandler.handle context: " + context);
        if ((context.getHandlerFlag() & CacheHandlerContext.DATASOURCE_HANDLER) == 0) {
            return null;
        }
        try {
            switch (context.getCacheAttribute().getType()) {
                case SedisConst.CACHE:
                case SedisConst.CACHE_EXPIRE:
                    if (invocation == null) {
                        LogUtil.info("MethodInvocation is null, context is: " + JsonUtil.beanToJson(context));
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
            LogUtil.error("DataSourceHandlerError, the context is " + JsonUtil.beanToJson(context), e);
        }
        return null;
    }

    private void submitTask(CacheHandlerContext context) {
        final CacheInterceptor interceptor = context.getInterceptor();
        if (interceptor == null) {
            LogUtil.warn("CacheInterceptor is null, will not submit a CacheTask: " + JsonUtil.beanToJson(context));
            return;
        }

        List<Integer> types = new ArrayList<Integer>(2);
        types.add(SedisConst.CACHE_EXPIRE);
        types.add(SedisConst.CACHE);
        interceptor.submit(interceptor, context.getCacheAttribute(), context.getKey(), types);
    }


}
