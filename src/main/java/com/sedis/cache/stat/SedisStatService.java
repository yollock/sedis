package com.sedis.cache.stat;

import com.sedis.cache.spring.AnnotationCacheAttributeSource;
import com.sedis.cache.spring.CacheAttribute;
import com.sedis.cache.spring.CacheInterceptor;
import com.sedis.util.JsonUtil;
import com.sedis.util.MapComparator;
import com.sedis.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by yollock on 2017/5/19.
 * ---------- query --------------
 * 访问所有的缓存注解配置 :: AnnotationCacheAttributeSource, 从IOC容器中获取
 * 访问MemoryCacheHandler的缓存情况 :: 根据注解构造key,然后从CacheInterceptor中获取Pileline,然后获取MemoryCacheHandler实例
 * 访问redis :: 获取CacheInterceptor实例, 就可以获取SedisClient
 * ------------ update ------------
 * 更新注解配置 ::
 * ------------ delete ------------
 * 删除注解配置 ::
 */
public class SedisStatService implements StatMBean {

    private final static SedisStatService instance = new SedisStatService();
    private boolean resetEnable;
    public final static int SUCCESS = 1;
    public final static int ERROR = -1;

    private final static int DEFAULT_PAGE = 1;
    private final static int DEFAULT_PER_PAGE_COUNT = Integer.MAX_VALUE;
    private static final String ORDER_TYPE_DESC = "desc";
    private static final String ORDER_TYPE_ASC = "asc";
    private static final String DEFAULT_ORDER_TYPE = ORDER_TYPE_ASC;
    private static final String DEFAULT_ORDERBY = "SQL";

    public static SedisStatService instance() {
        return instance;
    }

    @Override
    public String service(String url) {
        Map<String, String> parameters = getParameters(url);
        if (url.startsWith("/cacheAttribute.json")) {
            return result(SUCCESS, cacheAttribute());
        }
        return result(ERROR, "Do not support this request, please contact with administrator.");
    }

    private List<CacheAttribute> cacheAttribute() {
        final AnnotationCacheAttributeSource attributeSource = (AnnotationCacheAttributeSource) cacheInterceptor().getCacheAttributeSource();
        final Map<Object, CacheAttribute> cacheAttributeMap = attributeSource.getAttributeCache();

        List<CacheAttribute> cacheAttributes = new ArrayList<CacheAttribute>(cacheAttributeMap.size());
        for (CacheAttribute cacheAttribute : cacheAttributeMap.values()) {
            if (StringUtil.isEmpty(cacheAttribute.getKey())) {
                continue;
            }
            cacheAttributes.add(cacheAttribute);
        }
        return cacheAttributes;
    }

    private CacheInterceptor cacheInterceptor() {
        return (CacheInterceptor) CacheInterceptor.applicationContext.getBean("com.sedis.cache.spring.CacheInterceptor#0");
    }

    private static String result(int resultCode, Object content) {
        Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
        dataMap.put("ResultCode", resultCode);
        dataMap.put("Content", content);
        return JsonUtil.beanToJson(dataMap);
    }

    private List<Map<String, Object>> comparatorOrderBy(List<Map<String, Object>> array, Map<String, String> parameters) {
        // when open the stat page before executing some sql
        if (array == null || array.isEmpty()) {
            return null;
        }

        // when parameters is null
        String orderBy, orderType = null;
        Integer page = DEFAULT_PAGE;
        Integer perPageCount = DEFAULT_PER_PAGE_COUNT;
        if (parameters == null) {
            orderBy = DEFAULT_ORDERBY;
            orderType = DEFAULT_ORDER_TYPE;
            page = DEFAULT_PAGE;
            perPageCount = DEFAULT_PER_PAGE_COUNT;
        } else {
            orderBy = parameters.get("orderBy");
            orderType = parameters.get("orderType");
            String pageParam = parameters.get("page");
            if (pageParam != null && pageParam.length() != 0) {
                page = Integer.parseInt(pageParam);
            }
            String pageCountParam = parameters.get("perPageCount");
            if (pageCountParam != null && pageCountParam.length() > 0) {
                perPageCount = Integer.parseInt(pageCountParam);
            }
        }

        // others,such as order
        orderBy = orderBy == null ? DEFAULT_ORDERBY : orderBy;
        orderType = orderType == null ? DEFAULT_ORDER_TYPE : orderType;

        if (!ORDER_TYPE_DESC.equals(orderType)) {
            orderType = ORDER_TYPE_ASC;
        }

        // orderby the statData array
        if (orderBy.trim().length() != 0) {
            Collections.sort(array, new MapComparator<String, Object>(orderBy, ORDER_TYPE_DESC.equals(orderType)));
        }

        // page
        int fromIndex = (page - 1) * perPageCount;
        int toIndex = page * perPageCount;
        if (toIndex > array.size()) {
            toIndex = array.size();
        }

        return array.subList(fromIndex, toIndex);
    }

    private static Map<String, String> getParameters(String url) {
        if (url == null || (url = url.trim()).length() == 0) {
            return Collections.<String, String>emptyMap();
        }

        String parametersStr = StringUtil.subString(url, "?", null);
        if (parametersStr == null || parametersStr.length() == 0) {
            return Collections.<String, String>emptyMap();
        }

        String[] parametersArray = parametersStr.split("&");
        Map<String, String> parameters = new LinkedHashMap<String, String>();

        for (String parameterStr : parametersArray) {
            int index = parameterStr.indexOf("=");
            if (index <= 0) {
                continue;
            }
            String name = parameterStr.substring(0, index);
            String value = parameterStr.substring(index + 1);
            parameters.put(name, value);
        }
        return parameters;
    }

    public void setResetEnable(boolean resetEnable) {
        this.resetEnable = resetEnable;
    }
}
