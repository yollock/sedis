package com.sedis.cache.pipeline;

import com.sedis.cache.spring.CacheInterceptor;

import java.util.ArrayList;
import java.util.List;

/**
 * pipeline持有所有的handler,且handler的排序是固定的,
 * 从头往后遍历,根据注解的配置决定是否从当前handler获取缓存数据,
 * 获取失败后,调用下一个handler,直到获取到数据,
 * 然后反向遍历handler,获取失败的handler,且是有效handler,则将数据缓存到miss的handler.
 * <p/>
 * 考虑到miss的情况,获取其他状态,此时获取缓存数据就变成了有状态的行为,需要将状态隔离,
 * 声明类CachePipelineContext,每次访问DefaultCachePipeline时,都传入进来
 * <p/>
 * 加锁的考虑:
 * 1.锁的粒度是key
 * 2.热点key的pipeline加锁粒度
 * 目前有3层handler处理数据,当热点key发生时,从第一层memory开始加锁,显然加锁粒度是最大的,3个handler处于加锁中
 * 如果从redishandler加锁,则只有redishandler和datasourcehandler加锁
 * 如果从datasourcehandler加锁,则只有datasourcehandler加锁
 */
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


}
