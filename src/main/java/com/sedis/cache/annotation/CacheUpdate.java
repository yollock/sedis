package com.sedis.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 实现方案:
 * 1.在拦截器中保存实现查询的MethodInvocation
 * 2.在pipeline更新(datasource是更新, redis和memory是删除)后, 发布事件, 调用查询的MethodInvocation
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheUpdate {
    // 缓存key
    String key() default "";

    // 是否支持内存缓存，默认关闭
    boolean memoryEnable() default false;

    // 是否支持Redis缓存，默认关闭
    boolean redisEnable() default false;

    // 是否支持数据库内存，默认开启
    boolean dataSourceEnable() default true;
}
