package com.sedis.util.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

public interface DistributedLock {

	/**
	 * 阻塞式获取锁
	 * 
	 * @param key
	 */
	void lock(String key);

	/**
	 * 中断式获取锁
	 * 
	 * @param key
	 * @throws InterruptedException
	 */
	void lockInterruptibly(String key) throws InterruptedException;

	/**
	 * 一次性获取锁
	 * 
	 * @param key
	 * @return
	 */
	boolean tryLock(String key);

	/**
	 * 超时式获取锁
	 * 
	 * @param key
	 * @param timeout
	 * @param unit
	 * @return
	 * @throws InterruptedException
	 */
	boolean tryLock(String key, long timeout, TimeUnit unit) throws InterruptedException;

	/**
	 * 解锁
	 * 
	 * @param key
	 */
	void unlock(String key);

	/**
	 * 返回条件等待器
	 * 
	 * @return
	 */
	Condition newCondition(String key);
}
