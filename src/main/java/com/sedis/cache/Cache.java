package com.sedis.cache;

import com.sedis.cache.exception.CacheException;

public interface Cache<K, V> {

	public V get(K key) throws CacheException;

	public V put(K key, V value) throws CacheException;

	public V remove(K key) throws CacheException;

	public void clear() throws CacheException;

	public int size();
}
