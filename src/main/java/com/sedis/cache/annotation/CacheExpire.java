package com.sedis.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheExpire {
    // 缓存key
    String key() default "";

    // 是否支持内存缓存，默认关闭
    boolean memoryEnable() default false;

    // 是否支持Redis缓存，默认关闭
    boolean redisEnable() default false;

    // 是否支持数据库内存，默认开启
    boolean dataSourceEnable() default true;
}
