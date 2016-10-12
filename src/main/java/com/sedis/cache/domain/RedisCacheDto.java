package com.sedis.cache.domain;

/**
 * Created by yangbo12 on 2016/9/13.
 */
public class RedisCacheDto<V> extends CacheDto<V> {

    /**
     * array 1
     * list  2
     * map   3
     */
    private int type;
    private Class<?> vec;
    private boolean array;
    private String arrayJson;

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public Class<?> getVec() {
        return vec;
    }

    public void setVec(Class<?> vec) {
        this.vec = vec;
    }

    public boolean isArray() {
        return array;
    }

    public void setArray(boolean array) {
        this.array = array;
    }

    public String getArrayJson() {
        return arrayJson;
    }

    public void setArrayJson(String arrayJson) {
        this.arrayJson = arrayJson;
    }

    @Override
    public String toString() {
        return "RedisCacheDto{" +
                "type=" + type +
                ", vec=" + vec +
                ", array=" + array +
                ", arrayJson='" + arrayJson + '\'' +
                '}';
    }
}
