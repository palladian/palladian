package ws.palladian.helper.html;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * A helper class for handling JsonPath queries.
 * </p>
 * 
 * @author David Urbansky
 */
public final class JPathHelper {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(JPathHelper.class);

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
    public static <T> T get(Object json, String jPath, Class<T> targetClass) {
        Validate.notNull(json, "json must not be null.");
        Validate.notEmpty(jPath, "jPath must not be empty.");
        Validate.notNull(targetClass, "targetClass must not be null");

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

            return get(object, shorterPath, targetClass);
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.error(e.getMessage(), e);
        }

        return null;
    }

    public static <T> T get(String json, String jPath, Class<T> targetClass) {
        Validate.notNull(json, "json must not be null");
        Validate.notEmpty(jPath, "jPath must not be empty");
        Validate.notNull(targetClass, "targetClass must not be null");

        try {
            if (json.trim().startsWith("[")) {
                return get(new JSONArray(json), jPath, targetClass);
            } else {
                return get(new JSONObject(json), jPath, targetClass);
            }
        } catch (JSONException e) {
            LOGGER.error(e.getMessage(), e);
        }
        return null;
    }

}
