package com.sedis.util;

/**
 * Created by yollock on 2017/5/19.
 */
public abstract class StringUtil {

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

    private StringUtil() {
    }
}
