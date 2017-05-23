package com.sedis.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Created by yollock on 2017/5/19.
 */
public abstract class StringUtil {

    private final static Log logger = LogFactory.getLog(StringUtil.class);

    public static boolean isEmpty(CharSequence value) {
        if (value == null || value.length() == 0) {
            return true;
        }

        return false;
    }

    /**
     * Example: subString("abcd","a","c")="b"
     *
     * @param src
     * @param start null while start from index=0
     * @param to    null while to index=src.length
     * @return
     */
    public static String subString(String src, String start, String to) {
        int indexFrom = start == null ? 0 : src.indexOf(start);
        int indexTo = to == null ? src.length() : src.indexOf(to);
        if (indexFrom < 0 || indexTo < 0 || indexFrom > indexTo) {
            return null;
        }

        if (null != start) {
            indexFrom += start.length();
        }

        return src.substring(indexFrom, indexTo);

    }

    /**
     * Example: subString("12345","1","4")=23
     *
     * @param src
     * @param start
     * @param to
     * @return
     */
    public static Integer subStringToInteger(String src, String start, String to) {
        return stringToInteger(subString(src, start, to));
    }

    public static Integer stringToInteger(String in) {
        if (in == null) {
            return null;
        }
        in = in.trim();
        if (in.length() == 0) {
            return null;
        }

        try {
            return Integer.parseInt(in);
        } catch (NumberFormatException e) {
            logger.warn("stringToInteger fail,string=" + in, e);
            return null;
        }
    }

    private StringUtil() {
    }
}
