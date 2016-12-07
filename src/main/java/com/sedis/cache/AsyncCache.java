package com.sedis.cache.annotation;

import com.sedis.cache.exception.CacheException;

public interface AsyncCache<K, V> extends Cache {

	public V asyncGet(K key) throws CacheException;

	public V asyncPut(K key, V value) throws CacheException;

	public V asyncRemove(K key) throws CacheException;

}
