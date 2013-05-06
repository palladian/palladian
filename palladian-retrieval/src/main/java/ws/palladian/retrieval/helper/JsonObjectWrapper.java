package ws.palladian.retrieval.helper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.html.JPathHelper;

/**
 * <p>
 * Allow more convenient access to JSONObject without throwing exceptions but returning null in case elements were not
 * present.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class JsonObjectWrapper {

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
        return JPathHelper.get(jsonObject, path, targetClass);
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
