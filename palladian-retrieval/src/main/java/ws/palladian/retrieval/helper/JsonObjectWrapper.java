package ws.palladian.retrieval.helper;

import org.json.JSONArray;
import org.json.JSONException;
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

}
