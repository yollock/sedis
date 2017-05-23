package com.sedis.util;

import java.util.Collection;

/**
 * Created by yollock on 2017/5/23.
 */
public abstract class CollectionUtil {

    public static boolean isEmpty(Collection c) {
        return c == null || c.size() == 0;
    }



    private CollectionUtil() {
    }
}
