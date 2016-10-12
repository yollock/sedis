package com.sedis.cache.keytool;

import java.lang.reflect.Method;

public interface CacheKeyGenerator {
    public String generateKey(Method method, Object[] args, Object target, String key);
}
