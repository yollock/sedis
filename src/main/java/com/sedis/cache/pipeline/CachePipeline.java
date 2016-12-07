package com.sedis.cache.pipeline;

/**
 * Created by yollock on 2016/9/13.
 */
public interface CachePipeline {

    public <V> V handle(CacheHandlerContext context);

}
