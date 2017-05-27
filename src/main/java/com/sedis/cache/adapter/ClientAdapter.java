package com.sedis.cache.adapter;

/**
 * Created by yollock on 2017/5/27.
 */
public interface ClientAdapter {

    String set(String key, String value);

    String setex(String key, int seconds, String value);

    String get(String key);

    Long del(String key);

}
