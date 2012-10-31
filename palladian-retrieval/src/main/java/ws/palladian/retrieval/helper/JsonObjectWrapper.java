package ws.palladian.retrieval.helper;

import org.json.JSONArray;
import org.json.JSONObject;

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

    public JSONObject getJsonObject() {
        return jsonObject;
    }

    public void setJsonObject(JSONObject jsonObject) {
        this.jsonObject = jsonObject;
    }

    public String getString(String key) {
        String result = null;
        if (jsonObject.has(key)) {
            try {
                result = jsonObject.getString(key);
            } catch (Exception e) {
            }
        }

        return result;
    }

    public Integer getInt(String key) {
        Integer result = null;
        if (jsonObject.has(key)) {
            try {
                result = jsonObject.getInt(key);
            } catch (Exception e) {
            }
        }

        return result;
    }

    public Boolean getBoolean(String key) {
        Boolean result = null;
        if (jsonObject.has(key)) {
            try {
                result = jsonObject.getBoolean(key);
            } catch (Exception e) {
            }
        }

        return result;
    }

    public Double getDouble(String key) {
        Double result = null;
        if (jsonObject.has(key)) {
            try {
                result = jsonObject.getDouble(key);
            } catch (Exception e) {
            }
        }

        return result;
    }

    public Long getLong(String key) {
        Long result = null;
        if (jsonObject.has(key)) {
            try {
                result = jsonObject.getLong(key);
            } catch (Exception e) {
            }
        }

        return result;
    }

    public Object getObject(String key) {
        Object result = null;
        if (jsonObject.has(key)) {
            try {
                result = jsonObject.get(key);
            } catch (Exception e) {
            }
        }

        return result;
    }

    public JSONObject getJSONObject(String key) {
        JSONObject result = null;
        if (jsonObject.has(key)) {
            try {
                result = jsonObject.getJSONObject(key);
            } catch (Exception e) {
            }
        }

        return result;

    }

    public JSONArray getJSONArray(String key) {
        JSONArray result = null;
        if (jsonObject.has(key)) {
            try {
                result = jsonObject.getJSONArray(key);
            } catch (Exception e) {
            }
        }

        return result;
    }

    public void put(String key, Object value) {
        try {
            jsonObject.put(key, value);
        } catch (Exception e) {
        }
    }

}
