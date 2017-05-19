package com.sedis.cache.domain;

import java.util.Collection;
import java.util.Map;

/**
 * Created by yollock on 2016/9/13.
 */
public class RedisCacheDto<V> extends CacheDto<V> {

    /**
     * element 0
     * array   1
     * list    2
     * map     3
     */
    private int type;
    private String json;

    private Class<?> ec; // element class
    private Class<? extends Collection> cc; // collection class
    private Class<? extends Map> mc; // map class
    private Class<?> mkc; // map key class

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public Class<?> getEc() {
        return ec;
    }

    public void setEc(Class<?> ec) {
        this.ec = ec;
    }

    public Class<? extends Collection> getCc() {
        return cc;
    }

    public void setCc(Class<? extends Collection> cc) {
        this.cc = cc;
    }

    public Class<? extends Map> getMc() {
        return mc;
    }

    public void setMc(Class<? extends Map> mc) {
        this.mc = mc;
    }

    public Class<?> getMkc() {
        return mkc;
    }

    public void setMkc(Class<?> mkc) {
        this.mkc = mkc;
    }
}
