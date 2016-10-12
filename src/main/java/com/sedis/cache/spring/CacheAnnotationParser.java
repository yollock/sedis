package com.sedis.cache.spring;

import java.lang.reflect.AnnotatedElement;

/**
 * Created by yangbo12 on 2016/9/12.
 */
public interface CacheAnnotationParser {

    CacheAttribute parseCacheAnnotation(AnnotatedElement ae);

}
