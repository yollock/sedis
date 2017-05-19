package com.sedis.test;

import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.SimpleBeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;

/**
 * Created by yollock on 2016/9/25.
 */
public class SedisNamespaceHandlerTest {

    public static void main(String[] args) {
        DefaultResourceLoader loader = new DefaultResourceLoader();
        Resource resource = loader.getResource("src/test/resources/spring-context.xml");
        BeanDefinitionRegistry registry = new SimpleBeanDefinitionRegistry();
        BeanDefinitionReader reader = new XmlBeanDefinitionReader(registry);

        int count = reader.loadBeanDefinitions(resource);
        String[] beanDefinitionNames = reader.getRegistry().getBeanDefinitionNames();
        System.out.println("----------------------");
        for (String name : beanDefinitionNames) {
            System.out.println(name);
        }
    }
}

// <sedis:annotation-driven/>
// org.springframework.CACHE.interceptor.CacheInterceptor#0
// org.springframework.CACHE.config.internalCacheAdvisor
// org.springframework.CACHE.annotation.AnnotationCacheOperationSource#0
// org.springframework.aop.config.internalAutoProxyCreator

// <aop:aspectj-autoproxy/>
// org.springframework.aop.config.internalAutoProxyCreator

// <tx:annotation-driven/>
// springframework.transaction.interceptor.TransactionInterceptor#0
// org.springframework.transaction.annotation.AnnotationTransactionAttributeSource#0
// org.springframework.transaction.config.internalTransactionAdvisor
// org.springframework.aop.config.internalAutoProxyCreator

// 都放开
// org.springframework.CACHE.interceptor.CacheInterceptor#0
// org.springframework.transaction.interceptor.TransactionInterceptor#0
// org.springframework.transaction.annotation.AnnotationTransactionAttributeSource#0
// org.springframework.transaction.config.internalTransactionAdvisor
// org.springframework.CACHE.config.internalCacheAdvisor
// org.springframework.CACHE.annotation.AnnotationCacheOperationSource#0
// org.springframework.aop.config.internalAutoProxyCreator 基础自动代理对象只会创建一个