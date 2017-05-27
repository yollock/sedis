package com.sedis.cache.pipeline;

import com.sedis.cache.adapter.ClientAdapter;
import com.sedis.cache.common.SedisConst;
import com.sedis.cache.domain.RedisCacheDto;
import com.sedis.cache.spring.CacheInterceptor;
import com.sedis.util.JsonUtil;
import com.sedis.util.LogUtil;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;

import java.util.Collection;
import java.util.Map;

public class RedisCacheHandler extends AbstractCacheHandler {

    private static final int NEXT = 3;

    private ClientAdapter sedisClient;

    public RedisCacheHandler() {
        super();
    }

    public RedisCacheHandler(CacheInterceptor interceptor) {
        super();
        this.sedisClient = interceptor.getSedisClient();
    }

    @Override
    public <V> V handle(CacheHandlerContext context) {
        LogUtil.debug("RedisCacheHandler.handle context: " + context);
        final CacheHandler nextHandler = context.getHandlers().get(NEXT);
        if ((context.getHandlerFlag() & CacheHandlerContext.REDIS_HANDLER) == 0) {
            return nextHandler.handle(context);
        }
        if (sedisClient == null) {
            LogUtil.warn("redis client is null, get cache from next level");
            return nextHandler.handle(context);
        }

        switch (context.getCacheAttribute().getType()) {
            case SedisConst.CACHE:
                return cache(context, nextHandler);
            case SedisConst.CACHE_EXPIRE:
                cacheExpire(context);
                return nextHandler.handle(context);
            case SedisConst.CACHE_UPDATE:
                return nextHandler.handle(context);
            default:
                return null;
        }
    }

    private void cacheExpire(CacheHandlerContext context) {
        sedisClient.del(context.getKey());
        LogUtil.debug("succeed in deleting redis cache, key = " + context.getKey());
    }

    private <V> V cache(CacheHandlerContext context, CacheHandler nextHandler) {
        final String key = context.getKey();
        V result = null; // avoid the case: datasource result ok, but redis error
        try {
            RedisCacheDto rcd = this.getFromRedisAndConvert(key);
            if (rcd == null || System.currentTimeMillis() > rcd.getEt()) {
                LogUtil.info("get cache from redis, null or expired, get cache from next level, key is " + key);
                result = nextHandler.handle(context);
                if (result == null) {
                    return null;
                }
                rcd = new RedisCacheDto();
                rcd.setKey(key);
                rcd.setJson(JsonUtil.beanToJson(result));
                parseAndFillValueType(rcd, result);
                final long expireTime = context.getCacheAttribute().getRedisExpiredTime();
                rcd.setEt(System.currentTimeMillis() + expireTime);
                if (expireTime <= 0) {
                    sedisClient.set(key, JsonUtil.beanToJson(rcd)); // save json not val
                } else {
                    sedisClient.setex(key, new Long(expireTime / 1000).intValue(), JsonUtil.beanToJson(rcd));
                }
            }
            return (V) rcd.getVal();
        } catch (Throwable e) {
            LogUtil.error("RedisCacheHandler.cache, the context is " + JsonUtil.beanToJson(context), e);
            return result;
        }
    }

    private <V> RedisCacheDto<V> getFromRedisAndConvert(String key) {
        RedisCacheDto<V> rcd;
        try {
            final String rcdJson = sedisClient.get(key);
            if (rcdJson == null || rcdJson.trim().isEmpty()) {
                return null;
            }
            rcd = JsonUtil.jsonToBean(rcdJson, RedisCacheDto.class);
            final int type = rcd.getType();
            TypeFactory typeFactory = TypeFactory.defaultInstance();
            String json = rcd.getJson();
            JavaType javaType = null;
            if (type == 0) { // element
                javaType = typeFactory.constructType(rcd.getEc());
                rcd.setVal((V) JsonUtil.jsonToBean(json, javaType));
                return rcd;
            } else if (type == 1) { // array
                javaType = typeFactory.constructArrayType(rcd.getEc());
            } else if (type == 2) { // collection
                javaType = typeFactory.constructCollectionType(rcd.getCc(), rcd.getEc());
            } else if (type == 3) { // map
                javaType = typeFactory.constructMapType(rcd.getMc(), rcd.getMkc(), rcd.getEc());
            }
            if (json.length() <= 0 || javaType == null) {
                return rcd;
            }
            rcd.setVal((V) JsonUtil.jsonToBean(json, javaType));
        } catch (Throwable e) {
            rcd = null;
            LogUtil.error("RedisCacheHandler convert error, the key is " + key, e);
        }
        return rcd;
    }

    private void parseAndFillValueType(RedisCacheDto rcd, Object value) {
        rcd.setEc(null);
        rcd.setCc(null);
        rcd.setMc(null);
        rcd.setMkc(null);
        rcd.setType(0);
        if (value == null) {
            return;
        }
        if (value.getClass().isArray()) {
            rcd.setType(1);
            Object[] arrayValue = (Object[]) value;
            if (arrayValue.length > 0) {
                rcd.setEc(arrayValue[0].getClass());
            }
        } else if (value instanceof Collection) {
            rcd.setType(2);
            Collection collectionValue = (Collection) value;
            if (collectionValue.size() > 0) {
                rcd.setCc(collectionValue.getClass());
                rcd.setEc(collectionValue.iterator().next().getClass());
            }
        } else if (value instanceof Map) {
            rcd.setType(3);
            Map mapValue = (Map) value;
            if (mapValue.size() > 0) {
                Map.Entry entry = (Map.Entry) mapValue.entrySet().iterator().next();
                rcd.setMc(mapValue.getClass());
                rcd.setMkc(entry.getKey().getClass());
                rcd.setEc(entry.getValue().getClass());
            }
        } else {
            rcd.setEc(value.getClass());
        }
    }

}
