package com.sedis.cache.pipeline;

/**
 * Created by yollock on 2016/9/13.
 */
public interface CachePipeline {

    <V> V handle(CacheHandlerContext context);

    void destroy();
}
