package com.sedis.cache.domain;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 缓存的特性:
 * 1.失效控制
 * 2.简单统计,比如命中次数,访问次数
 * 3.流控(考虑使用Guava的RateLimiter)
 * 4.缓存傀儡值,保护DB(暂时不支持)
 * 5.并发控制,至少保证单JVM中,锁的粒度为key
 * 6.
 */
public class CacheDto<V> implements java.io.Serializable {

    // 缓存数据的key
    private String key;
    // 缓存的值
    private V val;
    // 缓存第一次生成的时间,也是生效时间
    private long st = System.currentTimeMillis();
    // 失效时间,注解中的参数决定
    private long et = 0;
    // 命中次数,同一个JVM保证了原子性,但分布式环境没有保证,考虑到分布式锁的性能问题,这只是一个概数统计
    private AtomicLong ht = new AtomicLong(0L);
    // 访问次数,同一个JVM保证了原子性,但分布式环境没有保证,考虑到分布式锁的性能问题,这只是一个概数统计
//    private AtomicLong vt = new AtomicLong(0L);

    public CacheDto() {
        super();
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public V getVal() {
        return val;
    }

    public void setVal(V val) {
        this.val = val;
    }

    public long getSt() {
        return st;
    }

    public void setSt(long st) {
        this.st = st;
    }

    public long getEt() {
        return et;
    }

    public void setEt(long et) {
        this.et = et;
    }

    public AtomicLong getHt() {
        return ht;
    }

    public void setHt(AtomicLong ht) {
        this.ht = ht;
    }

//    public AtomicLong getVt() {
//        return vt;
//    }
//
//    public void setVt(AtomicLong vt) {
//        this.vt = vt;
//    }
}