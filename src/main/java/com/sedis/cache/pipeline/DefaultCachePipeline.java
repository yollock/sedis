package com.sedis.cache.pipeline;

import com.sedis.cache.spring.CacheInterceptor;
import com.sedis.util.CollectionUtil;

import java.util.ArrayList;
import java.util.List;

public class DefaultCachePipeline implements CachePipeline {

    List<CacheHandler> defaultHandlers = null;

    public DefaultCachePipeline(CacheInterceptor interceptor) {
        List<CacheHandler> tempHandlers = new ArrayList<CacheHandler>(3);
        tempHandlers.add(new LockHandler(interceptor));
        tempHandlers.add(new MemoryCacheHandler(interceptor));
        tempHandlers.add(new RedisCacheHandler(interceptor));
        defaultHandlers = tempHandlers;
    }

    @Override
    public <V> V handle(CacheHandlerContext context) {
        List<CacheHandler> handlers = new ArrayList<CacheHandler>(defaultHandlers.size() + 1);
        handlers.addAll(defaultHandlers);
        handlers.add(new DataSourceHandler(context.getInvocation()));
        context.setHandlers(handlers);
        return (V) handlers.get(0).handle(context);
    }

    @Override
    public void destroy() {
        if (CollectionUtil.isEmpty(defaultHandlers)) {
            return;
        }
        for (CacheHandler handler : defaultHandlers) {
            handler.destroy();
        }
    }


}
