package com.sedis.cache.pipeline;

import com.sedis.util.locks.ExpireLock;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;

/**
 * Created by yollock on 2016/9/13.
 */
public abstract class AbstractCacheHandler implements CacheHandler {

    private static Logger logger = Logger.getLogger(AbstractCacheHandler.class);

    public static final int MIN_COUNT = 10000;

    public static final long MIN_PERIOD = 5L * 60L * 1000L;

    // 减小锁的粒度,同时,对每个key加锁,减少并发量,避免热点key
    private static final ConcurrentHashMap<String, ExpireLock> locks = new ConcurrentHashMap<String, ExpireLock>();

    public static ExpireLock getLock(String key) {
        ExpireLock lock = locks.get(key);
        if (lock == null) {
            ExpireLock newLock = new ExpireLock();
            ExpireLock oldLock = locks.putIfAbsent(key, newLock);
            if (oldLock == null) {
                return newLock;
            } else {
                return oldLock;
            }
        } else {
            return lock;
        }
    }

    // 清道夫
    public static ScheduledExecutorService servicer = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread("SedisLockScavenger");
        }
    });

    public static class ScavengeWorker implements Runnable {
        private int lockCount;
        private long maxPeriod;

        public ScavengeWorker() {
        }

        public ScavengeWorker(int lockCount, long maxPeriod) {
            this.lockCount = lockCount;
            this.maxPeriod = maxPeriod;
        }

        @Override
        public void run() {
            clearExpiredLock(lockCount, maxPeriod);
        }

    }

    private static void clearExpiredLock(int lockCount, long maxPeriod) {
        int count = lockCount;
        long period = maxPeriod;
        if (lockCount < MIN_COUNT) {
            count = MIN_COUNT;
        }
        if (maxPeriod < MIN_PERIOD) {
            period = MIN_PERIOD;
        }
        int size = locks.size();
        if (size < count) {
            return;
        }
        logger.info("开始执行lock清理,清理前的数量为" + size);
        for (Map.Entry<String, ExpireLock> entry : locks.entrySet()) {
            final String key = entry.getKey();
            final ExpireLock lock = entry.getValue();
            if (lock.isExpired(period)) {
                locks.remove(entry.getKey());
                size--;
            }
        }
        logger.info("结束执行lock清理,清理后的数量为" + size);

    }
}
