package com.sedis.cache.pipeline;

public interface CacheHandler {

    <V> V handle(CacheHandlerContext context);

    void destroy();
}
