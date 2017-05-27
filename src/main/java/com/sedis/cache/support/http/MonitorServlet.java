package com.sedis.cache.support.http;

import com.sedis.cache.stat.SedisStatService;
import com.sedis.util.LogUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import javax.servlet.ServletException;

/**
 * Created by yollock on 2017/5/19.
 */
public class MonitorServlet extends ResourceServlet {

    public static final String PARAM_NAME_RESET_ENABLE = "resetEnable";
    private SedisStatService statService = SedisStatService.instance();

    public MonitorServlet() {
        super("support/http/resource");
    }

    public void init() throws ServletException {
        super.init();

        try {
            String param = getInitParameter(PARAM_NAME_RESET_ENABLE);
            if (param != null && param.trim().length() != 0) {
                param = param.trim();
                boolean resetEnable = Boolean.parseBoolean(param);
                statService.setResetEnable(resetEnable);
            }
        } catch (Exception e) {
            String msg = "initParameter config error, resetEnable : " + getInitParameter(PARAM_NAME_RESET_ENABLE);
            LogUtil.error(msg, e);
        }
    }

    @Override
    protected String process(String fullUrl) {
        return statService.service(fullUrl);
    }
}
