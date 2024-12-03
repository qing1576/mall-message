package com.mall.util;

import com.google.gson.Gson;

/**
 * <b><u>GsonUtil功能说明：</u></b>
 * <p></p>
 * @author
 * 2024/11/7 10:01
 */
public class GsonUtil {

    private static final Gson gson = new Gson();

    /**
     * 将对象转成json字符串
     * @param object
     * @return
     */
    public static String toJson(Object object) {
        return gson.toJson(object);
    }
}
