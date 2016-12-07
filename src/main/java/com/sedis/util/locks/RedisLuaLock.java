package com.sedis.util.locks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;

/**
 * 待完成
 */
public class RedisLuaLock implements DistributedLock, java.io.Serializable {

	private static final long serialVersionUID = -4864423577822768437L;

	@Override
	public void lock(String key) {
		// TODO Auto-generated method stub

	}

	@Override
	public void lockInterruptibly(String key) throws InterruptedException {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean tryLock(String key) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean tryLock(String key, long time, TimeUnit unit) throws InterruptedException {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public void unlock(String key) {
		// TODO Auto-generated method stub

	}

	@Override
	public Condition newCondition(String key) {
		// TODO Auto-generated method stub
		return null;
	}

}
