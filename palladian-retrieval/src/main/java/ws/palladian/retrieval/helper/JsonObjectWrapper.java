package ws.palladian.retrieval.helper;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.parser.json.JsonObject;

/**
 * <p>
 * Allow more convenient access to JSONObject without throwing exceptions but returning null in case elements were not
 * present.
 * </p>
 * 
 * @author David Urbansky
 * @deprecated Use {@link JsonObject} instead.
 */
@Deprecated
public class JsonObjectWrapper {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(JsonObjectWrapper.class);

    private JSONObject jsonObject;

    public JsonObjectWrapper(JSONObject jsonObject) {
        super();
        this.jsonObject = jsonObject;
    }

    public JsonObjectWrapper() {
        this.jsonObject = new JSONObject();
    }

    public JsonObjectWrapper(String jsonString) {
        super();
        try {
            this.jsonObject = new JSONObject(jsonString);
        } catch (JSONException e) {
            this.jsonObject = new JSONObject();
        }
    }

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    /**
     * <p>
     * Allows you to access json objects using a "json path". The following could be accessed with "entry/a"
     * </p>
     * 
     * <pre>
     * {
     *   'entry': {
     *     'a': 1,
     *     'b': 2
     *   }
     * }
     * </pre>
     * 
     * @param path The path to query.
     * @param targetClass The expected type of the target item (the last item in the path).
     * @return
     * @throws JSONException
     */
    public <T> T get(String path, Class<T> targetClass) throws JSONException {
        return get(jsonObject, path, targetClass);
    }

    @Deprecated
    private static <T> T get(JSONObject json, String jPath, Class<T> targetClass) {
        return getWithObject(json, jPath, targetClass);
    }

    /**
     * <p>
     * Allows you to access json objects using a "json path". The following could be accessed with "entry/a"
     * </p>
     * 
     * <pre>
     * {
     *   'entry': {
     *     'a': 1,
     *     'b': 2
     *   }
     * }
     * </pre>
     * 
     * @param json The json object.
     * @param jPath The path.
     * @param targetClass The expected type of the target item (the last item in the path).
     * @return The targeted data.
     */
    private static <T> T getWithObject(Object json, String jPath, Class<T> targetClass) {
        Validate.notNull(json, "json must not be null.");
        Validate.notEmpty(jPath, "jPath must not be empty.");
        Validate.notNull(targetClass, "targetClass must not be null");

        if (jPath.startsWith("/")) {
            jPath = jPath.substring(1, jPath.length());
        }

        try {
            String[] split = jPath.split("/");

            Object object = null;
            String currentItem = split[0];

            // if arrays are used like "b[0][1][3]" we resolve them
            List<String> arrayIndices = StringHelper.getRegexpMatches("(?<=\\[)\\d+?(?=\\])", currentItem);
            currentItem = currentItem.replaceAll("\\[.*\\]", "");

            if (json instanceof JSONObject) {
                object = ((JSONObject)json).get(currentItem);

            } else if (json instanceof JSONArray) {
                object = ((JSONArray)json).get(0);
            }

            // resolve arrays
            for (String index : arrayIndices) {
                object = ((JSONArray)object).get(Integer.valueOf(index));
            }

            if (split.length == 1) {
                Object returnObject = null;
                try {
                    returnObject = targetClass.cast(object);
                } catch (Exception e) {
                    if (targetClass == String.class) {
                        returnObject = String.valueOf(object);
                    } else if (targetClass == Integer.class) {
                        returnObject = Integer.valueOf(object.toString());
                    } else if (targetClass == Long.class) {
                        returnObject = Long.valueOf(object.toString());
                    } else if (targetClass == Double.class) {
                        returnObject = Double.valueOf(object.toString());
                    }
                }
                return targetClass.cast(returnObject);
            }

            String shorterPath = StringUtils.join(split, "/", 1, split.length);

            return getWithObject(object, shorterPath, targetClass);
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        return null;
    }


    public String getString(String key) {
        String result = null;
        try {
            if (jsonObject.has(key)) {
                result = jsonObject.getString(key);
            }
        } catch (Exception e) {
        }

        return result;
    }

    public Integer getInt(String key) {
        Integer result = null;
        try {
            if (jsonObject.has(key)) {
                result = jsonObject.getInt(key);
            }
        } catch (Exception e) {
        }

        return result;
    }

    public Boolean getBoolean(String key) {
        Boolean result = null;
        try {
            if (jsonObject.has(key)) {
                result = jsonObject.getBoolean(key);
            }
        } catch (Exception e) {
        }

        return result;
    }

    public Double getDouble(String key) {
        Double result = null;
        try {
            if (jsonObject.has(key)) {
                result = jsonObject.getDouble(key);
            }
        } catch (Exception e) {
        }

        return result;
    }

    public Long getLong(String key) {
        Long result = null;
        try {
            if (jsonObject.has(key)) {
                result = jsonObject.getLong(key);
            }
        } catch (Exception e) {
        }

        return result;
    }

    public Object getObject(String key) {
        Object result = null;
        try {
            if (jsonObject.has(key)) {
                result = jsonObject.get(key);
            }
        } catch (Exception e) {
        }

        return result;
    }

    public JsonObjectWrapper getJSONObject(String key) {
        JsonObjectWrapper result = null;
        try {
            if (jsonObject.has(key)) {
                result = new JsonObjectWrapper(jsonObject.getJSONObject(key));
            }
        } catch (Exception e) {
        }

        return result;

    }

    public JSONArray getJSONArray(String key) {
        JSONArray result = null;
        try {
            if (jsonObject.has(key)) {
                result = jsonObject.getJSONArray(key);
            }
        } catch (Exception e) {
        }

        return result;
    }

    public void put(String key, Object value) {
        try {
            jsonObject.put(key, value);
        } catch (Exception e) {
        }
    }

    @Override
    public String toString() {
        return jsonObject.toString();
    }

    public static void main(String[] args) throws JSONException {
        String string = "{'entry': {'a': 1,'b':['1a',['one','two'],{'f':1.48}],'c': {'d':'2b'}}}";
        JsonObjectWrapper json = new JsonObjectWrapper(string);
        // System.out.println(json.get("entry", JSONObject.class));
        // System.out.println(json.get("entry/a", Integer.class));
        // System.out.println(json.get("entry/c/d", String.class));
        System.out.println(json.get("entry/b[0]", String.class));
        System.out.println(json.get("entry/b[1][1]", String.class));
        System.out.println(json.get("entry/b[2]/f", Double.class));
    }

}
