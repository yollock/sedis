package com.sedis.cache.spring;

import com.sedis.cache.annotation.Cache;
import com.sedis.cache.keytool.CacheKeyGenerator;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.AnnotatedElement;

/**
 * Created by yangbo12 on 2016/9/12.
 */
public class SpringCacheAnnotationParser implements CacheAnnotationParser {

    @Override
    public CacheAttribute parseCacheAnnotation(AnnotatedElement ae) {
        Cache ann = AnnotationUtils.getAnnotation(ae, Cache.class);
        if (ann == null) {
            return null;
        }
        return parseCacheAnnotation(ann);
    }

    public CacheAttribute parseCacheAnnotation(Cache cache) {
        return new CacheAttribute(cache.key(),//
                cache.memoryEnable(),//
                cache.memoryExpiredTime(),//
                cache.redisEnable(),//
                cache.redisExpiredTime(), //
                cache.dataSourceEnable());
    }

}
