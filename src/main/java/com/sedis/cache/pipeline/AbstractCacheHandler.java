package com.sedis.cache.pipeline;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by yangbo12 on 2016/9/13.
 */
public abstract class AbstractCacheHandler implements CacheHandler {

    // 减小锁的粒度,同时,对每个key加锁,减少并发量,避免热点key
    // 此处有可能形成瓶颈，因为多个表的缓存，都会公用这个容器，且不会被删除
    // 即使使用WeakReference,也会导致GC的误伤,GC的精度不够
    // 设计成latch,不仅满足key的粒度,也不会造成内存泄漏.
    private static ConcurrentMap<String, ReentrantLock> locks = new ConcurrentHashMap<String, ReentrantLock>();

    public static ReentrantLock getLock(String key) {
        ReentrantLock lock = locks.get(key);
        if (lock == null) {
            ReentrantLock newLock = new ReentrantLock();
            ReentrantLock oldLock = locks.putIfAbsent(key, newLock);
            if (oldLock == null) {
                return newLock;
            } else {
                return oldLock;
            }
        } else {
            return lock;
        }
    }

}
