package com.sedis.util;


import com.sedis.cache.common.log.DefaultLogger;
import com.sedis.cache.common.log.Logger;

public abstract class LogUtil {

    private static Logger logger = new DefaultLogger();// 可以通过设置为不同logservice控制log行为。

    public static boolean isTraceEnabled() {
        return logger.isTraceEnabled();
    }

    public static boolean isDebugEnabled() {
        return logger.isDebugEnabled();
    }

    public static boolean isWarnEnabled() {
        return logger.isWarnEnabled();
    }

    public static boolean isErrorEnabled() {
        return logger.isErrorEnabled();
    }

    public static boolean isStatsEnabled() {
        return logger.isStatsEnabled();
    }

    public static void trace(String msg) {
        logger.trace(msg);
    }

    public static void debug(String msg) {
        logger.debug(msg);
    }

    public static void debug(String format, Object... argArray) {
        logger.debug(format, argArray);
    }

    public static void debug(String msg, Throwable t) {
        logger.debug(msg, t);
    }

    public static void info(String msg) {
        logger.info(msg);
    }

    public static void info(String format, Object... argArray) {
        logger.info(format, argArray);
    }

    public static void info(String msg, Throwable t) {
        logger.info(msg, t);
    }

    public static void warn(String msg) {
        logger.warn(msg);
    }

    public static void warn(String format, Object... argArray) {
        logger.warn(format, argArray);
    }

    public static void warn(String msg, Throwable t) {
        logger.warn(msg, t);
    }

    public static void error(String msg) {
        logger.error(msg);
    }

    public static void error(String format, Object... argArray) {
        logger.error(format, argArray);
    }

    public static void error(String msg, Throwable t) {
        logger.error(msg, t);
    }

    public static void accessLog(String msg) {
        logger.accessLog(msg);
    }

    public static void accessStatsLog(String msg) {
        logger.accessStatsLog(msg);
    }

    public static void accessStatsLog(String format, Object... argArray) {
        logger.accessStatsLog(format, argArray);
    }

    public static void accessProfileLog(String format, Object... argArray) {
        logger.accessProfileLog(format, argArray);
    }

    public static Logger getLogger() {
        return logger;
    }

    public static void setLogger(Logger logger) {
        LogUtil.logger = logger;
    }

    //-------------------private constructor------------------

    private LogUtil() {
    }

}
