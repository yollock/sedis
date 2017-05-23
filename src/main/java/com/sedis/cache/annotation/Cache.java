package com.sedis.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Cache {

    // 缓存key
    String key() default "";

    // 是否支持内存缓存，默认关闭
    boolean memoryEnable() default false;

    // 内存缓存的过期时间默认半小时
    long memoryExpiredTime() default 30 * 60 * 1000;

    // 是否支持Redis缓存，默认关闭
    boolean redisEnable() default false;

    // Redis缓存的过期时间, 默认 1 小时
    long redisExpiredTime() default 60 * 60 * 1000;

    // 是否支持数据库获取数据，默认开启
    boolean dataSourceEnable() default true;

}
