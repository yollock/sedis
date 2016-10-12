package com.sedis.cache.pipeline;

import com.sedis.cache.spring.CacheAttribute;
import org.aopalliance.intercept.MethodInvocation;

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
    // 当当前handler有效,但是没有命中时,表示是否要创建新的dto
    // 第一次,需要标记true,然后在back中创建,存储;
    // 第二次访问有值,标记为false,在back中不会创建,直接set覆盖
    private boolean redisMissed = false;
    private boolean memoryMissed = false;
    private Object result;
    private int handlerFlag;
    private ReentrantLock lock;

    public CacheHandlerContext() {
    }

    public CacheHandlerContext(CacheAttribute cacheAttribute, MethodInvocation invocation, String key, int handlerFlag) {
        this.cacheAttribute = cacheAttribute;
        this.invocation = invocation;
        this.key = key;
        this.handlerFlag = handlerFlag;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    public void setLock(ReentrantLock lock) {
        this.lock = lock;
    }

    public int getHandlerFlag() {
        return handlerFlag;
    }

    public void setHandlerFlag(int handlerFlag) {
        this.handlerFlag = handlerFlag;
    }

    public Object getResult() {
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
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

    public boolean getRedisMissed() {
        return redisMissed;
    }

    public void setRedisMissed(boolean redisMissed) {
        this.redisMissed = redisMissed;
    }

    public boolean getMemoryMissed() {
        return memoryMissed;
    }

    public void setMemoryMissed(boolean memoryMissed) {
        this.memoryMissed = memoryMissed;
    }

    @Override
    public String toString() {
        return "CacheHandlerContext{" +
                "lock=" + lock +
                ", cacheAttribute=" + cacheAttribute +
                ", invocation=" + invocation +
                ", key='" + key + '\'' +
                ", redisMissed=" + redisMissed +
                ", memoryMissed=" + memoryMissed +
                ", result=" + result +
                ", handlerFlag=" + handlerFlag +
                '}';
    }
}
