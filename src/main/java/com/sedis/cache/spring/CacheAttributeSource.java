package com.sedis.cache.spring;

import java.lang.reflect.Method;

/**
 * Created by yangbo12 on 2016/9/12.
 */
public interface CacheAttributeSource {

    CacheAttribute getCacheAttribute(Method method, Class<?> targetClass);

}
