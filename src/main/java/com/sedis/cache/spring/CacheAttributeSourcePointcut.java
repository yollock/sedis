package com.sedis.cache.spring;

import org.springframework.aop.support.StaticMethodMatcherPointcut;
import org.springframework.util.ObjectUtils;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * 此类负责根据切点表达式,过滤不匹配的Advisor
 * TransactionAttributeSourcePointcut
 */
public abstract class CacheAttributeSourcePointcut extends StaticMethodMatcherPointcut implements Serializable {

    @Override
    public boolean matches(Method method, Class<?> targetClass) {
        CacheAttributeSource tas = getCacheAttributeSource();
        return (tas == null || tas.getCacheAttribute(method, targetClass) != null);
    }

    protected abstract CacheAttributeSource getCacheAttributeSource();

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CacheAttributeSourcePointcut)) {
            return false;
        }
        CacheAttributeSourcePointcut otherPc = (CacheAttributeSourcePointcut) other;
        return ObjectUtils.nullSafeEquals(getCacheAttributeSource(), otherPc.getCacheAttributeSource());
    }

    @Override
    public int hashCode() {
        return CacheAttributeSourcePointcut.class.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + ": " + getCacheAttributeSource();
    }

}
