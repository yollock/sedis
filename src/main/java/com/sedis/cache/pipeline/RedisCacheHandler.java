package com.sedis.cache.pipeline;

import com.sedis.cache.domain.MemoryCacheDto;
import com.sedis.cache.domain.RedisCacheDto;
import com.sedis.cache.spring.CacheInterceptor;
import com.sedis.util.JsonUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.text.MessageFormat;
import java.util.concurrent.locks.ReentrantLock;

public class RedisCacheHandler implements CacheHandler {

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
        final CacheHandler nextHandler = context.getHandlers().get(NEXT);
        if ((context.getHandlerFlag() & CacheHandlerContext.REDIS_HANDLER) == 0) {
            return nextHandler.handle(context);
        }
        if (sedisClient == null) {
            logger.warn("Redis访问层有效,但redis客户端对象为null,将从下一层获取数据");
            return nextHandler.handle(context);
        }
        final String key = context.getKey();
        ShardedJedis jedis = null;
        try {
            jedis = sedisClient.getResource();
            if (jedis == null) {
                logger.warn("Redis访问层有效,从redis客户端获取的连接为null,将从下一层获取数据");
                return nextHandler.handle(context);
            }

            RedisCacheDto rcd = this.getFromRedisAndConvert(jedis, key);
            if (rcd == null || System.currentTimeMillis() > rcd.getEt()) {
                logger.info("从redis获取的数据,为空或者失效,从下一层获取数据, key = " + key);
                V result = nextHandler.handle(context);
                if (result == null) {
                    return null;
                }
                rcd = new RedisCacheDto();
                rcd.setKey(key);
                rcd.setVal(result);
                parseAndFillValueType(rcd, result);
                rcd.setEt(System.currentTimeMillis() + context.getCacheAttribute().getRedisExpiredTime());
            }
            rcd.getHt().incrementAndGet();
            jedis.set(key, JsonUtils.beanToJson(rcd));
            return (V) rcd.getVal();
        } catch (Throwable t) {
            logger.error("RedisCacheHandlerError, the context is " + JsonUtils.beanToJson(context), t);
        } finally {
            try {
                sedisClient.returnResource(jedis);
            } catch (Throwable t) {
            }
        }
        return null;
    }

    /**
     * 从redis中获取key对应的值，并转换成具体的对象
     * 1.list直接转换LinkedHashMap,直接返回
     * 2.array会转换成LinkedHashMap
     * 3.Map直接转换,直接返回
     * 4.原生类型,比如String,
     */
    private <V> RedisCacheDto<V> getFromRedisAndConvert(ShardedJedis jedis, String key) {
        RedisCacheDto<V> redisCacheDto = null;
        try {
            final String redisCacheDtoJson = jedis.get(key);
            if (redisCacheDtoJson == null || redisCacheDtoJson.trim().isEmpty()) {
                return null;
            }
            redisCacheDto = JsonUtils.jsonToBean(redisCacheDtoJson, RedisCacheDto.class);
            final int type = redisCacheDto.getType();
            if (type == 1) { // array
                String arrayJson = redisCacheDto.getArrayJson();
                TypeFactory typeFactory = TypeFactory.defaultInstance();
                JavaType javaType = typeFactory.constructArrayType(redisCacheDto.getVec());
                redisCacheDto.setVal((V) JsonUtils.jsonToBean(arrayJson, javaType));
                return redisCacheDto;
            } else {
                return redisCacheDto;
            }
        } catch (Throwable t) {
            redisCacheDto = null;
            logger.error("RedisCacheHandlerConvertError, the key is " + key, t);
        }
        return redisCacheDto;
    }

    private void parseAndFillValueType(RedisCacheDto redisCacheDto, Object value) {
        redisCacheDto.setVec(null);
        redisCacheDto.setArray(false);
        redisCacheDto.setType(0);
        if (value == null) {
            return;
        }
        if (value.getClass().isArray()) {
            redisCacheDto.setType(1);
            redisCacheDto.setArray(true);
            Object[] arrayValue = (Object[]) value;
            if (arrayValue.length > 0) {
                redisCacheDto.setVec(arrayValue[0].getClass());
            }
            redisCacheDto.setArrayJson(JsonUtils.beanToJson(value));
            redisCacheDto.setVal(null);
        }
    }

}
