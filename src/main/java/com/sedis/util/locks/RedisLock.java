package com.sedis.util.locks;

import com.sedis.util.SingleLruCache;
import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 利用redis实现基本的分布式锁<br>
 * <p/>
 * 锁的获取:<br>
 * <p/>
 * 1.生成当前线程可以控制的失效时间(originalExpire);<br>
 * 2.原子往redis插入key(senNx(key, originalExpire),只能插入一次),如果成功,说明获取了锁,返回;<br>
 * 3.插入失败,说明有其他实例的线程获取了锁,则第一次获取此key的过期时间(firstExpire);<br>
 * 4.如果firstExpire > 当前时间,说明key没有过期,当前线程不能获取锁;如果 firstExpire <
 * 当前时间,说明锁没有释放,当前线程可以获取锁;<br>
 * 4.将originalExpire原子设置为key的value(getSet(key,
 * originalExpire)),返回第二次获取到的过期时间(secondExpire);<br>
 * 5.如果firstExpire == secondExpire,说明是当前实例的当前线程成功获取到锁,返回;
 * 6.如果依然没有获取锁,睡眠随机时间,然后循环执行上述步骤.
 * <p/>
 * 锁的释放: <br>
 * 1.获取当前线程插入的过期时间(originalExpire);<br>
 * 2.获取key的过期时间(currentExpire);<br>
 * 3.如果originalExpire == currentExpire, 说明当前线程有效持有着锁, 删除key; <br>
 * 4.如果不想等,说明当前线程持有的锁过期了,不能删除,直接返回;<br>
 * <p/>
 * <p/>
 * 特殊说明: <br>
 * 1.如果针对同一个key,当前实例的线程和其他实例的线程完全同步, 且场景是同时开始获取锁,但锁已经被其他线程占有,而且处于超时状态,
 * 那么两个线程获取到的第一次过期时间 (firstExpire)是相同的,
 * 且第二次获取的过期时间(secondExpire)也是相同的,最终两个线程均获取锁成功.建议使用纳秒值做比较<br>
 * 2.持有锁的超时时间如果过长,会影响程序的吞吐量,建议根据实际环境设置合理的超时时间.
 */
public class RedisLock implements DistributedLock, java.io.Serializable {

    private static final long serialVersionUID = -2646970500598851453L;

    private static long EXPIRE_TIME; // redis锁超时，防止线程入锁后，其他线程死等

    private final ShardedJedisPool shardedJedisPool;

    private boolean fair = false;

    // private final SingleLruCache<String, ReentrantLock> locks;

    private static final ThreadLocal<LockContext> expireTimeLocal = new ThreadLocal<LockContext>() {
        @Override
        protected LockContext initialValue() {
            return null;
        }
    };

    private static class LockContext {
        private long originalExpire = 0;
        private ReentrantLock lock;

        public LockContext() {
        }

        public LockContext(long originalExpire, ReentrantLock lock) {
            this.originalExpire = originalExpire;
            this.lock = lock;
        }

        public long getOriginalExpire() {
            return originalExpire;
        }

        public void setOriginalExpire(long originalExpire) {
            this.originalExpire = originalExpire;
        }

        public ReentrantLock getLock() {
            return lock;
        }

        public void setLock(ReentrantLock lock) {
            this.lock = lock;
        }
    }

    public RedisLock(ShardedJedisPool shardedJedisPool) {
        this(shardedJedisPool, false, 10L * 1000L * 1000L * 1000L);
    }

    public RedisLock(ShardedJedisPool shardedJedisPool, boolean fair) {
        this(shardedJedisPool, fair, 10L * 1000L * 1000L * 1000L);
    }

    public RedisLock(ShardedJedisPool shardedJedisPool, boolean fair, long expireTime) {
        this.fair = fair;
        EXPIRE_TIME = expireTime;
        this.shardedJedisPool = shardedJedisPool;
        this.locks = new SingleLruCache<String, ReentrantLock>();
    }

    @Override
    public void lockInterruptibly(String key) throws InterruptedException {
        final ReentrantLock lock = this.getLock(key);
        lock.lockInterruptibly();
        final Thread t = Thread.currentThread();
        try {
            for (; ; ) {
                final String originalExpire = String.valueOf(System.nanoTime() + EXPIRE_TIME + 1);

                if (redisLock(key, originalExpire)) {
                    expireTimeLocal.set(new LockContext(Long.parseLong(originalExpire), lock));
                    return;
                }

                Thread.sleep(randomSleepTime());
                if (t.isInterrupted()) {
                    expireTimeLocal.remove();
                    throw new InterruptedException(String.format("线程 %s 获取key %s的分布式锁,发生了线程中断!!", t.getName(), key));
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Exception e) {
        } finally {
        }
    }

    @Override
    public boolean tryLock(String key) {
        final ReentrantLock lock = this.getLock(key);
        lock.lock();
        try {
            final String originalExpire = String.valueOf(System.nanoTime() + EXPIRE_TIME + 1);

            if (redisLock(key, originalExpire)) {
                expireTimeLocal.set(new LockContext(Long.parseLong(originalExpire), lock));
                return true;
            }
        } catch (Exception e) {
            expireTimeLocal.remove();
        } finally {
        }
        return false;
    }

    @Override
    public boolean tryLock(String key, long timeout, TimeUnit unit) throws InterruptedException {
        long millis = unit.toMillis(timeout);
        final ReentrantLock lock = this.getLock(key);
        lock.lockInterruptibly();
        try {
            while (true) {
                if (millis <= 0) {
                    return false;
                }
                long startTime = System.currentTimeMillis();
                final String originalExpire = String.valueOf(System.nanoTime() + EXPIRE_TIME + 1);

                if (redisLock(key, originalExpire)) {
                    expireTimeLocal.set(new LockContext(Long.parseLong(originalExpire), lock));
                    return true;
                }

                Thread.sleep(randomSleepTime());
                millis -= System.currentTimeMillis() - startTime;
            }
        } catch (Exception e) {
            expireTimeLocal.remove();
        } finally {
        }
        return false;
    }


    @Override
    public void lock(String key) {
        final ReentrantLock lock = this.getLock(key);
        lock.lock();
        try {
            for (; ; ) {
                final String originalExpire = String.valueOf(System.nanoTime() + EXPIRE_TIME + 1);

                if (redisLock(key, originalExpire)) {
                    expireTimeLocal.set(new LockContext(Long.parseLong(originalExpire), lock));
                    return;
                }

                Thread.sleep(randomSleepTime());
            }
        } catch (Exception e) {
            expireTimeLocal.remove();
        } finally {
        }
    }


    @Override
    public void unlock(String key) {
        final ShardedJedisPool shardedJedisPool = this.shardedJedisPool;
        final ShardedJedis shardedJedis = shardedJedisPool.getResource();
        final LockContext context = expireTimeLocal.get();
        if (context == null) {
            return;
        }
        final ReentrantLock lock = context.getLock();
        try {
            long originalExpire = context.getOriginalExpire();
            String currentExpire = shardedJedis.get(key);
//          System.out.println(Thread.currentThread() + ", originalExpire = " + originalExpire + ", currentExpire = " + currentExpire);
            if (currentExpire != null && originalExpire == Long.parseLong(currentExpire)) {
                shardedJedis.del(key);
//              System.out.println(MessageFormat.format("thread = {0} originalExpire = {1}, currentExpire = {2} key = {3}", Thread.currentThread(), originalExpire, currentExpire, key));
            }
        } catch (Exception e) {
            expireTimeLocal.remove();
        } finally {
            if (lock != null) {
                lock.unlock();
            }
            expireTimeLocal.remove();
            shardedJedisPool.returnResource(shardedJedis);
        }
    }

    private boolean redisLock(String key, String originalExpire) {
        final ShardedJedisPool shardedJedisPool = this.shardedJedisPool;
        final ShardedJedis shardedJedis = shardedJedisPool.getResource();
        try {
            // 第一次原子性获取锁
            if (shardedJedis.setnx(key, originalExpire) == 1) {
                return true;
            }
            String firstExpire = shardedJedis.get(key);
            // 做缓存过期校验,如果过期,则进行第二次原子获取分布式锁
            // 过期校验:当前时间 - redis时间 > 过期时间段值
            if (firstExpire != null && System.nanoTime() - Long.parseLong(firstExpire) > EXPIRE_TIME) {
                String secondExpire = shardedJedis.getSet(key, originalExpire);
                if (secondExpire != null && secondExpire.equals(firstExpire)) {
                    return true;
                }
            }
            return false;
        } finally {
            shardedJedisPool.returnResource(shardedJedis);
        }
    }

    // 减小锁的粒度,同时,对每个key加锁,减少并发量,避免热点key
    private static SingleLruCache<String, ReentrantLock> locks = null;

    public static ReentrantLock getLock(String key) {
        ReentrantLock lock = locks.get(key);
        if (lock == null) {
            ReentrantLock newLock = new ReentrantLock();
            ReentrantLock oldLock = locks.putIfAbsent(key, newLock);
            if (oldLock == null) {
                return newLock;
            } else {
                return oldLock;
            }
        } else {
            return lock;
        }
    }

    /**
     * 获取锁失败后,线程睡眠的时间
     */
    private static final long SLEEP_TIME = 10L; // 获取锁失败后的睡眠时间

    private static final double[] SLEEP_FACTOR = {0.1, 0.2, 0.3};

    private static final Random SLEEP_RANDOM = new Random();

    private static long randomSleepTime() {
        return (long) (SLEEP_FACTOR[SLEEP_RANDOM.nextInt(SLEEP_FACTOR.length)] * SLEEP_TIME);
    }

    @Override
    public Condition newCondition(String key) {
        return this.getLock(key).newCondition();
    }

}
