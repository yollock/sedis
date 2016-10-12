package com.sedis.cache.pipeline;

import com.sedis.cache.domain.RedisCacheDto;
import com.sedis.common.util.JsonUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.type.TypeFactory;
import org.codehaus.jackson.type.JavaType;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.text.MessageFormat;
import java.util.concurrent.locks.ReentrantLock;

public class RedisCacheHandler extends AbstractCacheHandler {

    private static Logger logger = Logger.getLogger(AbstractCacheHandler.class);

    private ShardedJedisPool sedisClient;

    public RedisCacheHandler() {
        super();
    }

    public RedisCacheHandler(ShardedJedisPool sedisClient) {
        super();
        this.sedisClient = sedisClient;
    }

    @Override
    public <V> V forwardHandle(CacheHandlerContext context) {
        if ((context.getHandlerFlag() & CacheHandlerContext.REDIS_HANDLER) == 0) {
            return null;
        }
        if (sedisClient == null) {
            return null;
        }
        final String key = context.getKey();
        final ReentrantLock lock = RedisCacheHandler.getLock(key);
        ShardedJedis jedis = null;
        try {
            lock.lock();
            context.setLock(lock); // restore for reverseHandle
            jedis = sedisClient.getResource();
            if (jedis == null) {
                return null;
            }
            String val = jedis.get(key);
            if (val == null) {
                context.setRedisMissed(true);
                return null;
            } else {
                RedisCacheDto rcd = this.getFromRedisAndConvert(jedis, key);
                if (rcd == null) {
                    context.setRedisMissed(true);
                    return null;
                }
                if (System.currentTimeMillis() >= rcd.getEt()) {
                    context.setRedisMissed(true);
                    return null;
                }
                rcd.getHt().incrementAndGet();
                jedis.set(key, JsonUtils.beanToJson(rcd));
                return (V) rcd.getVal();
            }
        } catch (Throwable t) {
            logger.error(MessageFormat.format("RedisCacheHandlerError, the context is {0}", JsonUtils.beanToJson(context)), t);
        } finally {
            //lock.unlock();
            sedisClient.returnResource(jedis);
        }
        return null;
    }

    @Override
    public void reverseHandle(CacheHandlerContext context) {
        if ((context.getHandlerFlag() & CacheHandlerContext.REDIS_HANDLER) == 0) {
            return;
        }
        if (sedisClient == null) {
            return;
        }
        final String key = context.getKey();
        final ReentrantLock lock = context.getLock() == null ? RedisCacheHandler.getLock(key) : context.getLock();
        ShardedJedis jedis = null;
        try {
            lock.lock();
            jedis = sedisClient.getResource();
            if (jedis == null) {
                return;
            }
            if (context.getRedisMissed() && context.getResult() != null) {
                RedisCacheDto rcd = new RedisCacheDto();
                rcd.setKey(key);
                rcd.setVal(context.getResult());
                parseAndFillValueType(rcd, context.getResult());
                rcd.setEt(System.currentTimeMillis() + context.getCacheAttribute().getRedisExpiredTime());
                jedis.set(key, JsonUtils.beanToJson(rcd));
            }
        } catch (Throwable t) {
            logger.error(MessageFormat.format("RedisCacheHandlerError, the context is {0}", JsonUtils.beanToJson(context)), t);
        } finally {
            for (int i = 0, lockCount = lock.getHoldCount(); i < lockCount; i++) {
                lock.unlock();
            }
            sedisClient.returnResource(jedis);
        }
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
            logger.error(MessageFormat.format("RedisCacheHandlerConvertError, the key is {0}", key), t);
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
