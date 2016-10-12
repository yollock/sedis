package com.sedis.cache.spring.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * Created by yollock on 2016/9/23.
 */
public class SedisNamespaceHandler extends NamespaceHandlerSupport {


    @Override
    public void init() {
        registerBeanDefinitionParser("annotation-driven", new AnnotationDrivenSedisBeanDefinitionParser());
    }
}
