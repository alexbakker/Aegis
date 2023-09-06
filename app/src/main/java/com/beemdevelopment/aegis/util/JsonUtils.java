package com.beemdevelopment.aegis.util;

import javax.annotation.Nullable;
import org.json.JSONObject;

public class JsonUtils {
    private JsonUtils() {}

    @Nullable
    public static String optString(JSONObject obj, String key) {
        return obj.isNull(key) ? null : obj.optString(key, null);
    }
}
