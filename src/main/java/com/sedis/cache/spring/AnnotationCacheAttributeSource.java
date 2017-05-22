package com.sedis.cache.spring;

import org.springframework.util.Assert;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * 此类负责存储标注了@Cache的类和方法配置信息
 *
 * @AnnotationCacheAttributeSource
 */
public class AnnotationCacheAttributeSource extends AbstractFallbackCacheAttributeSource {

    private final Set<CacheAnnotationParser> annotationParsers;

    private final boolean publicMethodsOnly;

    public AnnotationCacheAttributeSource() {
        this(true);
    }

    public AnnotationCacheAttributeSource(boolean publicMethodsOnly) {
        this.publicMethodsOnly = publicMethodsOnly;
        this.annotationParsers = new LinkedHashSet<CacheAnnotationParser>(2);
        this.annotationParsers.add(new SpringCacheAnnotationParser());
    }

    public AnnotationCacheAttributeSource(CacheAnnotationParser annotationParser) {
        this.publicMethodsOnly = true;
        Assert.notNull(annotationParser, "CacheAnnotationParser must not be null");
        this.annotationParsers = Collections.singleton(annotationParser);
    }

    public AnnotationCacheAttributeSource(Set<CacheAnnotationParser> annotationParsers) {
        this.publicMethodsOnly = true;
        Assert.notEmpty(annotationParsers, "At least one CacheAnnotationParser needs to be specified");
        this.annotationParsers = annotationParsers;
    }

    @Override
    protected CacheAttribute findCacheAttribute(Method method) {
        return determineCacheAttribute(method);
    }

    @Override
    protected CacheAttribute findCacheAttribute(Class<?> clazz) {
        return determineCacheAttribute(clazz);
    }

    protected CacheAttribute determineCacheAttribute(AnnotatedElement ae) {
        for (CacheAnnotationParser annotationParser : this.annotationParsers) {
            CacheAttribute attr = annotationParser.parseCacheAnnotation(ae);
            if (attr != null) {
                return attr;
            }
        }
        return null;
    }
}
