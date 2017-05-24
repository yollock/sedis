package com.sedis.cache.pipeline;

import com.sedis.cache.common.SedisConst;
import com.sedis.cache.domain.RedisCacheDto;
import com.sedis.cache.spring.CacheInterceptor;
import com.sedis.util.JsonUtil;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.util.Collection;
import java.util.Map;

public class RedisCacheHandler extends AbstractCacheHandler {

    private static Logger logger = Logger.getLogger(RedisCacheHandler.class);

    private static final int NEXT = 3;

    private ShardedJedisPool sedisClient;

    public RedisCacheHandler() {
        super();
    }

    public RedisCacheHandler(CacheInterceptor interceptor) {
        super();
        this.sedisClient = interceptor.getSedisClient();
    }

    @Override
    public <V> V handle(CacheHandlerContext context) {
        logger.debug("RedisCacheHandler.handle context: " + context);
        final CacheHandler nextHandler = context.getHandlers().get(NEXT);
        if ((context.getHandlerFlag() & CacheHandlerContext.REDIS_HANDLER) == 0) {
            return nextHandler.handle(context);
        }
        if (sedisClient == null) {
            logger.warn("redis client is null, get cache from next level");
            return nextHandler.handle(context);
        }

        switch (context.getCacheAttribute().getType()) {
            case SedisConst.CACHE:
                return cache(context, nextHandler);
            case SedisConst.CACHE_EXPIRE:
                cacheExpire(context, nextHandler);
                return nextHandler.handle(context);
            case SedisConst.CACHE_UPDATE:
                return nextHandler.handle(context);
            default:
                return null;
        }
    }

    private void cacheExpire(CacheHandlerContext context, CacheHandler nextHandler) {
        ShardedJedis jedis = null;
        try {
            jedis = sedisClient.getResource();
            if (jedis == null) {
                logger.warn("redis connection from redis client is null, will not delete redis cache");
                return;
            }
            jedis.del(context.getKey());
            logger.debug("succeed in deleting redis cache, key = " + context.getKey());
        } catch (Throwable e) {
            logger.error("RedisCacheHandler.cacheExpire, the context is " + context, e);
        } finally {
            try {
                if (jedis != null) {
                    sedisClient.returnResource(jedis);
                }
            } catch (Throwable e) {
            }
        }
    }

    private <V> V cache(CacheHandlerContext context, CacheHandler nextHandler) {
        final String key = context.getKey();
        ShardedJedis jedis = null;
        V result = null;
        try {
            jedis = sedisClient.getResource();
            if (jedis == null) {
                logger.warn("redis connection from redis client is null, get cache from next level");
                return nextHandler.handle(context);
            }

            RedisCacheDto rcd = this.getFromRedisAndConvert(jedis, key);
            if (rcd == null || System.currentTimeMillis() > rcd.getEt()) {
                logger.info("get cache from redis, null or expired, get cache from next level, key is " + key);
                result = nextHandler.handle(context);
                if (result == null) {
                    return null;
                }
                rcd = new RedisCacheDto();
                rcd.setKey(key);
                rcd.setJson(JsonUtil.beanToJson(result));
                parseAndFillValueType(rcd, result);
                rcd.setEt(System.currentTimeMillis() + context.getCacheAttribute().getRedisExpiredTime());
            } else {
                result = (V) rcd.getVal();
            }
            rcd.getHt().incrementAndGet();
            rcd.setVal(null);
            jedis.set(key, JsonUtil.beanToJson(rcd)); // update count
            rcd.setVal(result);
            return (V) rcd.getVal();
        } catch (Throwable e) {
            logger.error("RedisCacheHandler.cache, the context is " + JsonUtil.beanToJson(context), e);
            return result;
        } finally {
            try {
                if (jedis != null) {
                    sedisClient.returnResource(jedis);
                }
            } catch (Throwable e) {
            }
        }
    }

    private <V> RedisCacheDto<V> getFromRedisAndConvert(ShardedJedis jedis, String key) {
        RedisCacheDto<V> rcd;
        try {
            final String rcdJson = jedis.get(key);
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
        } catch (Throwable t) {
            rcd = null;
            logger.error("RedisCacheHandler convert error, the key is " + key, t);
            t.printStackTrace();
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
            return;
        } else if (value instanceof Collection) {
            rcd.setType(2);
            Collection collectionValue = (Collection) value;
            if (collectionValue.size() > 0) {
                rcd.setCc(collectionValue.getClass());
                rcd.setEc(collectionValue.iterator().next().getClass());
            }
            return;
        } else if (value instanceof Map) {
            rcd.setType(3);
            Map mapValue = (Map) value;
            if (mapValue.size() > 0) {
                Map.Entry entry = (Map.Entry) mapValue.entrySet().iterator().next();
                rcd.setMc(mapValue.getClass());
                rcd.setMkc(entry.getKey().getClass());
                rcd.setEc(entry.getValue().getClass());
            }
            return;
        } else {
            rcd.setEc(value.getClass());
        }
    }

}
