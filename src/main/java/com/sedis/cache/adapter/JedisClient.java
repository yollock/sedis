package com.sedis.cache.adapter;

import com.sedis.cache.exception.CacheException;
import com.sedis.util.LogUtil;
import org.springframework.beans.factory.InitializingBean;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

/**
 * Created by yollock on 2017/5/27.
 */
public class JedisClient implements ClientAdapter, InitializingBean {

    private ShardedJedisPool shardedJedisPool;

    public JedisClient() {
    }

    public JedisClient(ShardedJedisPool shardedJedisPool) {
        this.shardedJedisPool = shardedJedisPool;
    }

    @Override
    public String set(String key, String value) {
        ShardedJedis jedis = null;
        try {
            jedis = shardedJedisPool.getResource();
            if (jedis == null) {
                LogUtil.warn("redis connection from ShardedJedisPool is null");
                return "";
            }
            return jedis.set(key, value);
        } catch (Throwable e) {
            LogUtil.warn("redis error: ", e);
        } finally {
            returnResource(jedis);
        }
        return "";
    }

    @Override
    public String setex(String key, int seconds, String value) {
        ShardedJedis jedis = null;
        try {
            jedis = shardedJedisPool.getResource();
            if (jedis == null) {
                LogUtil.warn("redis connection from ShardedJedisPool is null");
                return "";
            }
            return jedis.setex(key, seconds, value);
        } catch (Throwable e) {
            LogUtil.warn("redis error: ", e);
        } finally {
            returnResource(jedis);
        }
        return "";
    }

    @Override
    public String get(String key) {
        ShardedJedis jedis = null;
        try {
            jedis = shardedJedisPool.getResource();
            if (jedis == null) {
                LogUtil.warn("redis connection from ShardedJedisPool is null");
                return "";
            }
            return jedis.get(key);
        } catch (Throwable e) {
            LogUtil.warn("redis error: ", e);
        } finally {
            returnResource(jedis);
        }
        return "";
    }

    @Override
    public Long del(String key) {
        ShardedJedis jedis = null;
        try {
            jedis = shardedJedisPool.getResource();
            if (jedis == null) {
                LogUtil.warn("redis connection from ShardedJedisPool is null");
                return new Long(-1L);
            }
            return jedis.del(key);
        } catch (Throwable e) {
            LogUtil.warn("redis error: ", e);
        } finally {
            returnResource(jedis);
        }
        return new Long(-1L);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (shardedJedisPool == null) {
            throw new CacheException("ShardedJedisPool is null");
        }
    }

    private void returnResource(ShardedJedis jedis) {
        try {
            if (jedis != null) {
                shardedJedisPool.returnResource(jedis);
            }
        } catch (Throwable e) {
            LogUtil.warn("redis connection return to ShardedJedisPool error", e);
        }
    }

    public ShardedJedisPool getShardedJedisPool() {
        return shardedJedisPool;
    }

    public void setShardedJedisPool(ShardedJedisPool shardedJedisPool) {
        this.shardedJedisPool = shardedJedisPool;
    }
}
