package com.sedis.cache.stat;

import com.sedis.util.JsonUtil;
import com.sedis.util.StringUtil;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Created by yollock on 2017/5/19.
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

        return result(ERROR, "Do not support this request, please contact with administrator.");

    }

    public static String result(int resultCode, Object content) {
        Map<String, Object> dataMap = new LinkedHashMap<String, Object>();
        dataMap.put("resultCode", resultCode);
        dataMap.put("content", content);
        return JsonUtil.beanToJson(dataMap);
    }

    public static Map<String, String> getParameters(String url) {
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
