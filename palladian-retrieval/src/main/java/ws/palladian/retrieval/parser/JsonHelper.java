package ws.palladian.retrieval.parser;

import org.apache.commons.lang3.Validate;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>
 * Some convenience methods for dealing with JSON data types.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://json.org/java/">JSON in Java</a>
 */
public final class JsonHelper {

    private JsonHelper() {
        // utility class.
    }

    /**
     * <p>
     * Get a {@link String} from the supplied {@link JSONObject}, if no String with the specified name exists, return
     * <code>null</code>.
     * </p>
     * 
     * @param obj The JSONObject from which to get the String, not <code>null</code>.
     * @param name The name of the String in the JSONObject to get, not <code>null</code> or empty.
     * @return The String value with the specified name, or <code>null</code> if no such String exists.
     * @throws JSONException In case the JSONObject cannot be parsed correctly.
     */
    public static String getString(JSONObject obj, String name) throws JSONException {
        Validate.notEmpty(name, "name must not be empty");
        Validate.notNull(obj, "obj must not be null");

        String ret = null;
        if (obj.has(name)) {
            ret = obj.getString(name);
        }
        return ret;
    }

    /**
     * <p>
     * Get a {@link Integer} from the supplied {@link JSONObject}, if no Integer with the specified name exists, return
     * <code>null</code>.
     * </p>
     * 
     * @param obj The JSONObject from which to get the Integer, not <code>null</code>.
     * @param name The name of the Integer in the JSONObject to get, not <code>null</code> or empty.
     * @return The Integer value with the specified name, or <code>null</code> if no such Integer exists.
     * @throws JSONException In case the JSONObject cannot be parsed correctly.
     */
    public static Integer getInteger(JSONObject obj, String name) throws JSONException {
        Validate.notEmpty(name, "name must not be empty");
        Validate.notNull(obj, "obj must not be null");

        Integer ret = null;
        if (obj.has(name)) {
            ret = obj.getInt(name);
        }
        return ret;
    }

    /**
     * <p>
     * Get a {@link Long} from the supplied {@link JSONObject}, if no Long with the specified name exists, return
     * <code>null</code>.
     * </p>
     * 
     * @param obj The JSONObject from which to get the Long, not <code>null</code>.
     * @param name The name of the Long in the JSONObject to get, not <code>null</code> or empty.
     * @return The String value with the specified name, or <code>null</code> if no such Long exists.
     * @throws JSONException In case the JSONObject cannot be parsed correctly.
     */
    public static Long getLong(JSONObject obj, String name) throws JSONException {
        Validate.notEmpty(name, "name must not be empty");
        Validate.notNull(obj, "obj must not be null");

        Long ret = null;
        if (obj.has(name)) {
            ret = obj.getLong(name);
        }
        return ret;
    }

}
