package com.sedis.util.locks.test;

import com.sedis.util.locks.RedisLock;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by yollock on 2016/12/7.
 */
public class RedisLockTest {


    public static void main(String[] args) {
        String configPath = "classpath:spring-context.xml";
        ApplicationContext context = new ClassPathXmlApplicationContext(configPath);
        final ShardedJedisPool pool = (ShardedJedisPool) context.getBean("sedisClient");
        final int currency = 10;
        ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(currency);
        final RedisLock lock = new RedisLock(pool);
        final Counter counter = new Counter();
        final String key = "redis";
        final String countKey = "count";
        int count = 200000;
        final AtomicLong countless = new AtomicLong(count);
        for (int i = 0; i < count; i++) {
            final int finalI = i;
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    try {
                        long lockStart = System.currentTimeMillis();
                        lock.lock(key);
                        long lockEnd = System.currentTimeMillis();
                        ShardedJedis shardedJedis = pool.getResource();
                        try {
                            String val = shardedJedis.get(countKey);
                            if (val == null) {
                                val = "0";
                            }
                            long longval = Long.parseLong(val);
                            shardedJedis.set(countKey, ++longval + "");
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {
                            pool.returnResource(shardedJedis);
                        }

                        long unlockStart = System.currentTimeMillis();
                        lock.unlock(key);
                        long unlockEnd = System.currentTimeMillis();
                        countless.decrementAndGet();
                        //System.out.println(Thread.currentThread() + " , lockEnd - lockStart = " + (lockEnd - lockStart) + " and  unlockEnd - unlockStart = " + (unlockEnd - unlockStart));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }
        while (true) {
            if (countless.get() == 0) {
                break;
            }
            LockSupport.parkNanos(executor, 1000L * 1000L * 1000L);
            continue;
        }
        executor.shutdown();
        System.out.println(pool.getResource().get(key) + " " + counter.getVal());
    }

    public static class Counter {
        int val = 0;

        public void increase() {
            val++;
        }

        public int getVal() {
            return val;
        }
    }

}
