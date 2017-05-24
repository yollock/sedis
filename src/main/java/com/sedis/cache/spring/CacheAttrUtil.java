package com.sedis.cache.spring;

import com.google.common.base.Splitter;
import com.sedis.cache.keytool.CacheKeyGenerator;
import com.sedis.cache.keytool.DefaultCacheKeyGenerator;
import com.sedis.cache.pipeline.CacheHandlerContext;
import com.sedis.util.CollectionUtil;
import org.aopalliance.intercept.MethodInvocation;

import java.util.Collections;
import java.util.List;

/**
 * Created by yollock on 2016/9/13.
 */
public abstract class CacheAttrUtil {

    private static CacheKeyGenerator keyGenerator = new DefaultCacheKeyGenerator();

    private CacheAttrUtil() {
    }

    public static int getHandlerFlag(CacheAttribute cacheAttribute) {
        int handlerFlag = 0;
        if (cacheAttribute.getMemoryEnable()) {
            handlerFlag |= CacheHandlerContext.MEMORY_HANDLER;
        }
        if (cacheAttribute.getRedisEnable()) {
            handlerFlag |= CacheHandlerContext.REDIS_HANDLER;
        }
        if (cacheAttribute.getDataSourceEnable()) {
            handlerFlag |= CacheHandlerContext.DATASOURCE_HANDLER;
        }
        return handlerFlag;
    }

    public static String getUniqueKey(CacheAttribute cacheAttr, MethodInvocation invocation) {
        // Method method, Object[] args, Object target, String key
        return keyGenerator.generateKey(invocation.getMethod(), //
                invocation.getArguments(),//
                invocation.getThis(), //
                cacheAttr.getKey());
    }

    public static CacheAttribute copy(CacheAttribute cacheAttr) {
        return new CacheAttribute(cacheAttr.getKey(), //
                cacheAttr.getType(), //
                cacheAttr.getMemoryEnable(), //
                cacheAttr.getMemoryExpiredTime(), //
                cacheAttr.redisEnable, //
                cacheAttr.getRedisExpiredTime(), //
                cacheAttr.getDataSourceEnable() //
        );
    }

    public static String[] params(String uniqueKey) {
        List<String> temp = Splitter.on("@").splitToList(uniqueKey);
        if (CollectionUtil.isEmpty(temp)) {
            return new String[0];
        }
        String[] params = new String[temp.size() - 1];
        boolean isFirst = true;
        for (int i = 0, len = temp.size(); i < len; i++) {
            if (isFirst) {
                isFirst = false;
                continue;
            } else {
                params[i - 1] = temp.get(i);
            }
        }
        return params;
    }


}
