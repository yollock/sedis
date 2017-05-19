package com.sedis.cache.spring;

/**
 * Created by yollock on 2016/9/12.
 */
public class CacheAttribute {

    String key;
    /** 1@Cache, 2@CacheExpire */
    int type;
    boolean memoryEnable;
    long memoryExpiredTime;
    boolean redisEnable;
    long redisExpiredTime;
    boolean dataSourceEnable;

    public CacheAttribute() {
    }

    public CacheAttribute(String key, int type) {
        this.key = key;
        this.type = type;
    }

    public CacheAttribute(String key, int type, boolean memoryEnable, boolean redisEnable, boolean dataSourceEnable) {
        this.key = key;
        this.type = type;
        this.memoryEnable = memoryEnable;
        this.redisEnable = redisEnable;
        this.dataSourceEnable = dataSourceEnable;
    }

    public CacheAttribute(String key, //
                          int type, boolean memoryEnable, //
                          long memoryExpiredTime, //
                          boolean redisEnable, //
                          long redisExpiredTime,//
                          boolean dataSourceEnable//
    ) {
        this.key = key;
        this.type = type;
        this.memoryEnable = memoryEnable;
        this.memoryExpiredTime = memoryExpiredTime;
        this.redisEnable = redisEnable;
        this.redisExpiredTime = redisExpiredTime;
        this.dataSourceEnable = dataSourceEnable;
    }

    public String getKey() {
        return key;
    }

    public CacheAttribute setKey(String key) {
        this.key = key;
        return this;
    }

    public boolean getMemoryEnable() {
        return memoryEnable;
    }

    public CacheAttribute setMemoryEnable(boolean memoryEnable) {
        this.memoryEnable = memoryEnable;
        return this;
    }

    public long getMemoryExpiredTime() {
        return memoryExpiredTime;
    }

    public CacheAttribute setMemoryExpiredTime(long memoryExpiredTime) {
        this.memoryExpiredTime = memoryExpiredTime;
        return this;
    }

    public boolean getRedisEnable() {
        return redisEnable;
    }

    public CacheAttribute setRedisEnable(boolean redisEnable) {
        this.redisEnable = redisEnable;
        return this;
    }

    public long getRedisExpiredTime() {
        return redisExpiredTime;
    }

    public CacheAttribute setRedisExpiredTime(long redisExpiredTime) {
        this.redisExpiredTime = redisExpiredTime;
        return this;
    }

    public boolean getDataSourceEnable() {
        return dataSourceEnable;
    }

    public CacheAttribute setDataSourceEnable(boolean dataSourceEnable) {
        this.dataSourceEnable = dataSourceEnable;
        return this;
    }

    public int getType() {
        return type;
    }

    public CacheAttribute setType(int type) {
        this.type = type;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CacheAttribute that = (CacheAttribute) o;

        return key.equals(that.key);

    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
