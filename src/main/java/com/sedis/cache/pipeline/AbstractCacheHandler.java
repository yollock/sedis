package com.sedis.cache.pipeline;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by yangbo12 on 2016/9/13.
 */
public abstract class AbstractCacheHandler implements CacheHandler {

    // 减小锁的粒度,同时,对每个key加锁,减少并发量,避免热点key
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
