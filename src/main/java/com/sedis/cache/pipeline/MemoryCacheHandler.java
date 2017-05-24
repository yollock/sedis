package com.sedis.cache.pipeline;

import com.sedis.cache.common.SedisConst;
import com.sedis.cache.domain.MemoryCacheDto;
import com.sedis.cache.spring.CacheInterceptor;
import com.sedis.util.SingleLruCache;
import org.apache.log4j.Logger;


public class MemoryCacheHandler extends AbstractCacheHandler {

    private static Logger logger = Logger.getLogger(MemoryCacheHandler.class);

    private static final int NEXT = 2;

    private SingleLruCache<String, MemoryCacheDto<?>> cache = null;

    public MemoryCacheHandler(CacheInterceptor interceptor) {
        super();
        cache = new SingleLruCache<String, MemoryCacheDto<?>>(interceptor.getMemoryCount());
    }

    /**
     * 考虑三种情况:
     * 1 获取到当前的缓存,且有效(不为空,且不过期)
     * 2 获取到下一层缓存,不为空,只做更新,然后返回
     * 3 获取到下一层缓存,为空,直接返回null
     * -------------------------------------------
     * there are 3 conditions:
     * 1 get current level cache, and it is effective (not null, not expired)
     * 2 get next level cache, not null, so update only, then return
     * 3 get next level cache, null, return null
     *
     * @param context
     * @param <V>
     * @return
     */
    @Override
    public <V> V handle(CacheHandlerContext context) {
        final CacheHandler nextHandler = context.getHandlers().get(NEXT);
        if ((context.getHandlerFlag() & CacheHandlerContext.MEMORY_HANDLER) == 0) {
            return nextHandler.handle(context);
        }

        switch (context.getCacheAttribute().getType()) {
            case SedisConst.CACHE:
                return cache(context, nextHandler);
            case SedisConst.CACHE_EXPIRE:
                cache.remove(context.getKey());
                return nextHandler.handle(context);
            case SedisConst.CACHE_UPDATE:
                return nextHandler.handle(context);
            default:
                return null;
        }
    }

    private <V> V cache(CacheHandlerContext context, CacheHandler nextHandler) {
        logger.debug("MemoryCacheHandler.handle context: " + context);
        final String key = context.getKey();
        MemoryCacheDto mcd = cache.get(key);
        if (mcd == null || System.currentTimeMillis() > mcd.getEt()) {
            logger.info("get cache from memory, null or expired, so get from next level, the key = " + key);
            V result = nextHandler.handle(context);
            if (result == null) {
                return null;
            }
            mcd = new MemoryCacheDto();
            mcd.setKey(context.getKey());
            mcd.setVal(result);
            mcd.setEt(System.currentTimeMillis() + context.getCacheAttribute().getMemoryExpiredTime());
            cache.put(context.getKey(), mcd);
        }
        mcd.getHt().incrementAndGet();
        return (V) mcd.getVal();
    }

    @Override
    public void destroy() {
        super.destroy();
        cache.clear();
    }
}
