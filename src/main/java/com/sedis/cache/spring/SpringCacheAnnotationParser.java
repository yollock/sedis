package com.sedis.cache.spring;

import com.sedis.cache.annotation.Cache;
import com.sedis.cache.annotation.CacheExpire;
import com.sedis.cache.common.SedisConst;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.AnnotatedElement;

/**
 * Created by yollock on 2016/9/12.
 */
public class SpringCacheAnnotationParser implements CacheAnnotationParser {

    @Override
    public CacheAttribute parseCacheAnnotation(AnnotatedElement ae) {
        Cache cache = AnnotationUtils.getAnnotation(ae, Cache.class);
        if (cache != null) {
            return parseCacheAnnotation(cache, SedisConst.CACHE);
        }
        CacheExpire cacheExpire = AnnotationUtils.getAnnotation(ae, CacheExpire.class);
        if (cacheExpire != null) {
            return parseCacheExpireAnnotation(cacheExpire, SedisConst.CACHE_EXPIRE);
        }
        return null;
    }

    private CacheAttribute parseCacheExpireAnnotation(CacheExpire cacheExpire, int type) {
        return new CacheAttribute(cacheExpire.key(), //
                type, //
                cacheExpire.memoryEnable(), //
                cacheExpire.redisEnable(), //
                cacheExpire.dataSourceEnable() //
        );
    }

    public CacheAttribute parseCacheAnnotation(Cache cache, int type) {
        return new CacheAttribute(cache.key(),//
                type, //
                cache.memoryEnable(),//
                cache.memoryExpiredTime(),//
                cache.redisEnable(),//
                cache.redisExpiredTime(), //
                cache.dataSourceEnable() //
        );
    }

}
