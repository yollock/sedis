package com.sedis.cache.pipeline;

import com.sedis.cache.spring.CacheAttribute;
import org.aopalliance.intercept.MethodInvocation;

import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by yollock on 2016/9/22.
 */
public class CacheHandlerContext {

    public static int BOTTOM_HANDLER = 0;
    public static int MEMORY_HANDLER = 1; // 1
    public static int REDIS_HANDLER = 2; // 10
    public static int DATASOURCE_HANDLER = 4; // 100

    private CacheAttribute cacheAttribute;
    private MethodInvocation invocation;
    private String key;
    private int handlerFlag;

    private List<CacheHandler> handlers;

    public CacheHandlerContext() {
    }

    public CacheHandlerContext(CacheAttribute cacheAttribute, MethodInvocation invocation, String key, int handlerFlag) {
        this.cacheAttribute = cacheAttribute;
        this.invocation = invocation;
        this.key = key;
        this.handlerFlag = handlerFlag;
    }

    public List<CacheHandler> getHandlers() {
        return handlers;
    }

    public CacheAttribute getCacheAttribute() {
        return cacheAttribute;
    }

    public void setCacheAttribute(CacheAttribute cacheAttribute) {
        this.cacheAttribute = cacheAttribute;
    }

    public MethodInvocation getInvocation() {
        return invocation;
    }

    public void setInvocation(MethodInvocation invocation) {
        this.invocation = invocation;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public int getHandlerFlag() {
        return handlerFlag;
    }

    public void setHandlerFlag(int handlerFlag) {
        this.handlerFlag = handlerFlag;
    }

    public void setHandlers(List<CacheHandler> handlers) {
        this.handlers = handlers;
    }
}
