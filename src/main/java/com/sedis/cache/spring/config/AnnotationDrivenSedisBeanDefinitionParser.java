package com.sedis.cache.spring.config;

import com.sedis.cache.spring.AnnotationCacheAttributeSource;
import com.sedis.cache.spring.BeanFactoryCacheAttributeSourceAdvisor;
import com.sedis.cache.spring.CacheInterceptor;
import org.springframework.aop.config.AopNamespaceUtils;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.parsing.CompositeComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

public class AnnotationDrivenSedisBeanDefinitionParser implements BeanDefinitionParser {

    @Override
    public BeanDefinition parse(Element element, ParserContext parserContext) {
        AopAutoProxyConfigurer.configureAutoProxyCreator(element, parserContext);
        return null;
    }

    private static class AopAutoProxyConfigurer {

        public static void configureAutoProxyCreator(Element element, ParserContext parserContext) {
            AopNamespaceUtils.registerAutoProxyCreatorIfNecessary(parserContext, element);

            String sedisAdvisorBeanName = "com.sedis.cache.spring.config.internalSedisAdvisor";
            if (!parserContext.getRegistry().containsBeanDefinition(sedisAdvisorBeanName)) {
                Object eleSource = parserContext.extractSource(element);

                RootBeanDefinition sourceDef = new RootBeanDefinition(AnnotationCacheAttributeSource.class);
                sourceDef.setSource(eleSource);
                sourceDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                String sourceName = parserContext.getReaderContext().registerWithGeneratedName(sourceDef);

                RootBeanDefinition interceptorDef = new RootBeanDefinition(CacheInterceptor.class);
                interceptorDef.setSource(eleSource);
                interceptorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                interceptorDef.getPropertyValues().add("cacheAttributeSource", new RuntimeBeanReference(sourceName));
                if (element.hasAttribute("sedis-client") && StringUtils.hasLength(element.getAttribute("sedis-client"))) {
                    interceptorDef.getPropertyValues().add("sedisClient", new RuntimeBeanReference(element.getAttribute("sedis-client")));
                }
                if (element.hasAttribute("memory-count")) {
                    interceptorDef.getPropertyValues().add("memoryCount", element.getAttribute("memory-count"));
                }
                String interceptorName = parserContext.getReaderContext().registerWithGeneratedName(interceptorDef);

                RootBeanDefinition advisorDef = new RootBeanDefinition(BeanFactoryCacheAttributeSourceAdvisor.class);
                advisorDef.setSource(eleSource);
                advisorDef.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
                advisorDef.getPropertyValues().add("cacheAttributeSource", new RuntimeBeanReference(sourceName));
                advisorDef.getPropertyValues().add("adviceBeanName", interceptorName); // 单纯的字符串注入,不是对象注入
                if (element.hasAttribute("order")) {
                    advisorDef.getPropertyValues().add("order", element.getAttribute("order"));
                }
                parserContext.getRegistry().registerBeanDefinition(sedisAdvisorBeanName, advisorDef);

                CompositeComponentDefinition compositeDef = new CompositeComponentDefinition(element.getTagName(), eleSource);
                compositeDef.addNestedComponent(new BeanComponentDefinition(sourceDef, sourceName));
                compositeDef.addNestedComponent(new BeanComponentDefinition(interceptorDef, interceptorName));
                compositeDef.addNestedComponent(new BeanComponentDefinition(advisorDef, sedisAdvisorBeanName));
                parserContext.registerComponent(compositeDef);
            }
        }
    }

}
