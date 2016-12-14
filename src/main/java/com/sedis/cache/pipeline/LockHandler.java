package com.sedis.cache.pipeline;

import com.sedis.cache.spring.CacheInterceptor;
import com.sedis.util.locks.ExpireLock;
import org.apache.log4j.Logger;

import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.locks.Lock;

/**
 * Created by yollock on 2016/12/14.
 */
public class LockHandler implements CacheHandler {

    private static Logger logger = Logger.getLogger(LockHandler.class);

    private static final int NEXT = 1;

    public static final int MIN_COUNT = 10000;

    public static final long MIN_PERIOD = 5L * 60L * 1000L;

    // 减小锁的粒度,同时,对每个key加锁,减少并发量,避免热点key
    protected final ConcurrentHashMap<String, ExpireLock> locks = new ConcurrentHashMap<String, ExpireLock>();

    public ExpireLock getLock(String key) {
        ExpireLock lock = locks.get(key);
        if (lock == null) {
            locks.putIfAbsent(key, new ExpireLock());
            lock = locks.get(key);
        }
        return lock;
    }


    public LockHandler(CacheInterceptor interceptor) {
        servicer.schedule(new ScavengeWorker(interceptor.getLockCount(), interceptor.getMaxPeriod()), //
                interceptor.getDelay(), //
                TimeUnit.MILLISECONDS //
        );
    }


    public <V> V handle(CacheHandlerContext context) {
        final Lock lock = getLock(context.getKey());
        try {
            lock.lock();
            return context.getHandlers().get(NEXT).handle(context);
        } catch (Throwable t) {
            logger.warn("LockHandler error", t);
        } finally {
            lock.unlock();
        }
        return null;
    }


    // 清道夫
    private final ScheduledExecutorService servicer = new ScheduledThreadPoolExecutor(1, new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread("SedisLockScavenger_" + Thread.currentThread().getName());
        }
    });

    private class ScavengeWorker implements Runnable {
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

    private void clearExpiredLock(int lockCount, long maxPeriod) {
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
                locks.remove(key);
                size--;
            }
        }
        logger.info("结束执行lock清理,清理后的数量为" + size);

    }
}
