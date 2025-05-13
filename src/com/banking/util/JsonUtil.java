package com.banking.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Iterator;
import org.json.JSONObject;
import org.json.JSONException;

/**
 * Utility class for JSON operations using org.json library
 */
public class JsonUtil {

    /**
     * Parse JSON string into a Map
     * 
     * @param jsonString JSON string to parse
     * @return Map containing the parsed JSON data
     * @throws Exception if parsing fails
     */
    public static Map<String, Object> parseJson(String jsonString) throws Exception {
        Map<String, Object> result = new HashMap<>();

        try {
            JSONObject jsonObject = new JSONObject(jsonString);
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                Object value = jsonObject.get(key);

                if (JSONObject.NULL.equals(value)) {
                    result.put(key, null);
                } else {
                    result.put(key, value);
                }
            }

            return result;
        } catch (JSONException e) {
            throw new Exception("Failed to parse JSON: " + e.getMessage(), e);
        }
    }
}
