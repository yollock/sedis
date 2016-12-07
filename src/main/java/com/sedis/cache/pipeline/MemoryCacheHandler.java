package com.sedis.cache.pipeline;

import com.sedis.cache.domain.MemoryCacheDto;
import com.sedis.util.SingleLruCache;


public class MemoryCacheHandler extends AbstractCacheHandler {

    private SingleLruCache<String, MemoryCacheDto<?>> cache = null;

    public MemoryCacheHandler() {
        super();
    }

    public MemoryCacheHandler(int memoryCount) {
        super();
        cache = new SingleLruCache<String, MemoryCacheDto<?>>(memoryCount);
    }

    @Override
    public <V> V forwardHandle(CacheHandlerContext context) {
        if ((context.getHandlerFlag() & CacheHandlerContext.MEMORY_HANDLER) == 0) {
            return null;
        }
        final String key = context.getKey();
        MemoryCacheDto mcd = cache.get(key);
        if (mcd == null) {
            context.setMemoryMissed(true);
            return null;
        }
        // 如果当前缓存层的value过期,获取下一层数据
        if (System.currentTimeMillis() >= mcd.getEt()) {
            context.setMemoryMissed(true);
            return null;
        }
        mcd.getHt().incrementAndGet();
        return (V) mcd.getVal();

    }

    @Override
    public void reverseHandle(CacheHandlerContext context) {
        if ((context.getHandlerFlag() & CacheHandlerContext.MEMORY_HANDLER) == 0) {
            return;
        }
        // 如果缓存miss,且result不为空,说明在后面的handler获取了数据,则更新当前handler的数据
        if (context.getMemoryMissed() && context.getResult() != null) {
            MemoryCacheDto mcd = new MemoryCacheDto();
            mcd.setKey(context.getKey());
            mcd.setVal(context.getResult());
            mcd.setEt(System.currentTimeMillis() + context.getCacheAttribute().getMemoryExpiredTime());
            cache.put(context.getKey(), mcd);
        }
    }


}
