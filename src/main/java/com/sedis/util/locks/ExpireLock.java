package com.sedis.util.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by yollock on 2016/11/14.
 * 实现失效特性.
 */
public class ExpireLock extends ReentrantLock {

    private long millis;
    private int times;

    public ExpireLock() {
        super();
    }

    public ExpireLock(boolean fair) {
        super(fair);
    }

    @Override
    public void lock() {
        super.lock();
        millis = System.currentTimeMillis();
        times++;
    }

    @Override
    public void lockInterruptibly() throws InterruptedException {
        super.lockInterruptibly();
        millis = System.currentTimeMillis();
        times++;
    }

    @Override
    public boolean tryLock() {
        final boolean result = super.tryLock();
        if (result) {
            millis = System.currentTimeMillis();
            times++;
        }
        return result;
    }

    @Override
    public boolean tryLock(long timeout, TimeUnit unit) throws InterruptedException {
        final boolean result = super.tryLock(timeout, unit);
        if (result) {
            millis = System.currentTimeMillis();
            times++;
        }
        return result;
    }

    /**
     * 使用次数超过限制,返回true,表示可以删除
     *
     * @param times
     * @return
     */
    public boolean isExpired(int times) {
        return this.times > times;
    }

    /**
     * 使用时间超过约定时间间隔,返回true,表示可以删除
     *
     * @param period
     * @return
     */
    public boolean isExpired(long period) {
        return System.currentTimeMillis() - this.millis > period;
    }

    /**
     * 既要满足次数限制,也要满足时间间隔限制
     *
     * @param times
     * @param period
     * @return
     */
    public boolean isExpired(int times, long period) {
        return this.times > times && System.currentTimeMillis() - this.millis > period;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
