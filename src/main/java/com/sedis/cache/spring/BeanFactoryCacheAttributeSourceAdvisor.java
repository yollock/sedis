package com.sedis.cache.spring;

import org.springframework.aop.ClassFilter;
import org.springframework.aop.Pointcut;
import org.springframework.aop.support.AbstractBeanFactoryPointcutAdvisor;

/**
 * 此类负责为给定的bean获取指定的Advisor(这里是标注了@Cache的类方法),这样就有了给当前bean创建代理对象的机会
 *
 */
public class BeanFactoryCacheAttributeSourceAdvisor extends AbstractBeanFactoryPointcutAdvisor {

    private CacheAttributeSource cacheAttributeSource;

    private final CacheAttributeSourcePointcut pointcut = new CacheAttributeSourcePointcut() {
        @Override
        protected CacheAttributeSource getCacheAttributeSource() {
            return cacheAttributeSource;
        }
    };

    public void setCacheAttributeSource(CacheAttributeSource cacheAttributeSource) {
        this.cacheAttributeSource = cacheAttributeSource;
    }

    public void setClassFilter(ClassFilter classFilter) {
        this.pointcut.setClassFilter(classFilter);
    }

    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

}
