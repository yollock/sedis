package com.sedis.cache.pipeline;

import com.sedis.cache.spring.CacheAttribute;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Handler初始设计成单例,每次代理时,直接从CacheAttribute获取对应的注解信息,解析需要参与的Handler,
 * 以及存和查缓存数据时,需要追加考虑的属性
 * 1.和老设计比,属于CacheAttribute类的信息,都在CacheAttribute,属于Handler类的信息,放在Handler
 * 2.单例设计,避免代码复杂
 */
public interface CacheHandler {

    <V> V forwardHandle(CacheHandlerContext context);

    void reverseHandle(CacheHandlerContext context);
}
