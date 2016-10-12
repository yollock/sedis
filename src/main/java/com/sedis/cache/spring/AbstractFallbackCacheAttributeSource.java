package com.sedis.cache.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.util.ClassUtils;
import org.springframework.util.ObjectUtils;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by yangbo12 on 2016/9/12.
 */
public abstract class AbstractFallbackCacheAttributeSource implements CacheAttributeSource {

    protected final Log logger = LogFactory.getLog(getClass());

    private final static CacheAttribute NULL_CACHE_ATTRIBUTE = new CacheAttribute();

    final Map<Object, CacheAttribute> attributeCache = new ConcurrentHashMap<Object, CacheAttribute>();

    public CacheAttribute getCacheAttribute(Method method, Class<?> targetClass) {
        // First, see if we have a cached value.
        Object cacheKey = getCacheKey(method, targetClass);
        Object cached = this.attributeCache.get(cacheKey);
        if (cached != null) {
            // Value will either be canonical value indicating there is no transaction attribute,
            // or an actual transaction attribute.
            if (cached == NULL_CACHE_ATTRIBUTE) {
                return null;
            } else {
                return (CacheAttribute) cached;
            }
        } else {
            // We need to work it out.
            CacheAttribute txAtt = computeTransactionAttribute(method, targetClass);
            // Put it in the cache.
            if (txAtt == null) {
                this.attributeCache.put(cacheKey, NULL_CACHE_ATTRIBUTE);
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Adding transactional method '" + method.getName() + "' with attribute: " + txAtt);
                }
                this.attributeCache.put(cacheKey, txAtt);
            }
            return txAtt;
        }
    }

    protected Object getCacheKey(Method method, Class<?> targetClass) {
        return new DefaultCacheKey(method, targetClass);
    }

    private CacheAttribute computeTransactionAttribute(Method method, Class<?> targetClass) {
        // Don't allow no-public methods as required.
        if (allowPublicMethodsOnly() && !Modifier.isPublic(method.getModifiers())) {
            return null;
        }

        // Ignore CGLIB subclasses - introspect the actual user class.
        Class<?> userClass = ClassUtils.getUserClass(targetClass);
        // The method may be on an interface, but we need attributes from the target class.
        // If the target class is null, the method will be unchanged.
        Method specificMethod = ClassUtils.getMostSpecificMethod(method, userClass);
        // If we are dealing with method with generic parameters, find the original method.
        specificMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);

        // First try is the method in the target class.
        CacheAttribute txAtt = findTransactionAttribute(specificMethod);
        if (txAtt != null) {
            return txAtt;
        }

        // Second try is the transaction attribute on the target class.
        txAtt = findTransactionAttribute(specificMethod.getDeclaringClass());
        if (txAtt != null) {
            return txAtt;
        }

        if (specificMethod != method) {
            // Fallback is to look at the original method.
            txAtt = findTransactionAttribute(method);
            if (txAtt != null) {
                return txAtt;
            }
            // Last fallback is the class of the original method.
            return findTransactionAttribute(method.getDeclaringClass());
        }
        return null;
    }

    protected abstract CacheAttribute findTransactionAttribute(Method specificMethod);

    protected abstract CacheAttribute findTransactionAttribute(Class<?> clazz);

    protected boolean allowPublicMethodsOnly() {
        return false;
    }

    private static class DefaultCacheKey {

        private final Method method;

        private final Class targetClass;

        public DefaultCacheKey(Method method, Class targetClass) {
            this.method = method;
            this.targetClass = targetClass;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof DefaultCacheKey)) {
                return false;
            }
            DefaultCacheKey otherKey = (DefaultCacheKey) other;
            return (this.method.equals(otherKey.method) && ObjectUtils.nullSafeEquals(this.targetClass, otherKey.targetClass));
        }

        @Override
        public int hashCode() {
            return this.method.hashCode() * 29 + (this.targetClass != null ? this.targetClass.hashCode() : 0);
        }
    }


}
