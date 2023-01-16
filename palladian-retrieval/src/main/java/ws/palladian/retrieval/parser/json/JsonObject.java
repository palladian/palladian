package ws.palladian.retrieval.parser.json;

import com.jayway.jsonpath.JsonPath;
import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.Config;
import com.jsoniter.spi.JsoniterSpi;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import ws.palladian.helper.math.MathHelper;

import java.io.Serializable;
import java.util.*;

/**
 * <p>
 * A {@link JsonObject} is an unordered collection of name/value pairs. Its external form is a string wrapped in curly
 * braces with colons between the names and values, and commas between the values and names. The object conforms to the
 * {@link Map} interface, allowing map manipulation, iteration, lookup, etc. The typed getters (e.g.
 * getDouble(int)) allow retrieving values for the corresponding type, in case, the type in the JSON is
 * incompatible to the requested type, <code>null</code> is returned.
 * </p>
 *
 * <p>
 * A {@link JsonObject} constructor can be used to convert an external form JSON text into an internal form, or to
 * convert values into a JSON text using the <code>put</code> and <code>toString</code> methods.
 * </p>
 *
 * <p>
 * The texts produced by the <code>toString</code> methods strictly conform to the JSON syntax rules. The constructors
 * are more forgiving in the texts they will accept:
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just before the closing brace.</li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not begin with a quote or single quote, and if they do not
 * contain leading or trailing spaces, and if they do not contain any of these characters:
 * <code>{ } [ ] / \ : , #</code> and if they do not look like numbers and if they are not the reserved words
 * <code>true</code>, <code>false</code>, or <code>null</code>.</li>
 * </ul>
 *
 * <p>
 * This implementation is based on <a href="http://json.org/java/">JSON-java</a>, but it has been simplified to provide
 * more convenience, and unnecessary functionality has been stripped to keep the API clear. The most important changes
 * to the original implementation are as follows: 1) <code>opt</code> and <code>get</code> methods have been
 * consolidated, the getters return Objects, giving <code>null</code> in case no value for a given key/index was present
 * or the given datatype could no be converted. 2) <code>JsonArray</code> and <code>JsonObject</code> conform to the
 * Java Collections API (i.e. <code>JsonArray</code> implements the <code>List</code> interface, <code>JsonObject</code>
 * the <code>Map</code> interface). 3) Value retrieval is possible using a "JPath" through the <code>query</code>
 * methods. The JPath is inspired from XPath (much less expressive though) and allows digging into JSON structures using
 * one method call, thus avoiding chained method invocations and tedious <code>null</code> checks.
 * </p>
 *
 * @author JSON.org
 * @author Philipp Katz
 * @version 2013-06-17
 */
public class JsonObject extends AbstractMap<String, Object> implements Json, Serializable {
    /** The map where the JsonObject's properties are kept. */
    private final Object2ObjectMap<String, Object> map;

    /**
     * <p>
     * Construct an empty {@link JsonObject}.
     * </p>
     */
    public JsonObject() {
        map = new Object2ObjectLinkedOpenHashMap<>();
    }

    /**
     * <p>
     * Construct a {@link JsonObject} from a {@link Map}.
     * </p>
     *
     * @param map A map object that can be used to initialize the contents of the JsonObject.
     */
    public JsonObject(Map map) {
        this.map = new Object2ObjectLinkedOpenHashMap<>();
        if (map != null) {
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                if (value != null) {
                    this.map.put(key.toString(), value);
                }
            }
        }
    }

    /**
     * <p>
     * Try to construct a {@link JsonObject} from a source JSON text string. Instead of the constructor, this method
     * does not throw a {@link JsonException} in case the JsonObject cannot be constructed.
     * </p>
     *
     * @param source A string beginning with <code>{</code>&nbsp;<small>(left brace)</small> and ending with
     *               <code>}</code>&nbsp;<small>(right brace)</small>.
     * @return The {@link JsonObject}, or <code>null</code> in case it could not be parsed.
     */
    public static JsonObject tryParse(String source) {
        try {
            return new JsonObject(source);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * Construct a {@link JsonObject} from a source JSON text string. This is the most commonly used JsonObject
     * constructor.
     * </p>
     *
     * @param source A string beginning with <code>{</code>&nbsp;<small>(left brace)</small> and ending with
     *               <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JsonException If there is a syntax error in the source string or a duplicated key.
     */
    public JsonObject(String source) throws JsonException {
        Any any = JsonIterator.deserialize(source);
        map = any.as(Object2ObjectLinkedOpenHashMap.class);
    }

    /**
     * <p>
     * Get the value object associated with a key.
     * </p>
     *
     * @param key A key string.
     * @return An object value, or <code>null</code> in case there is no value with specified key.
     */
    @Override
    public Object get(Object key) {
        return key == null ? null : map.get(key);
    }

    /**
     * <p>
     * Get the {@link Boolean} value associated with a key.
     * </p>
     *
     * @param key A key string.
     * @return The boolean value, or <code>null</code> in case there is no value with specified key, or the value cannot
     * be parsed as boolean.
     * @throws JsonException
     */
    public boolean getBoolean(String key) throws JsonException {
        return JsonUtil.parseBoolean(this.get(key));
    }

    public Boolean tryGetBoolean(String key) {
        try {
            return getBoolean(key);
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean tryGetBoolean(String key, Boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * <p>
     * Get the {@link Double} value associated with a key.
     * </p>
     *
     * @param key A key string.
     * @return The double value, or <code>null</code> in case there is no value with specified key, or the value cannot
     * be parsed as Double.
     */
    public double getDouble(String key) throws JsonException {
        return JsonUtil.parseDouble(this.get(key));
    }

    public Double tryGetDouble(String key) {
        try {
            return getDouble(key);
        } catch (Exception e) {
            return null;
        }
    }

    public Double tryGetDouble(String key, Double defaultValue) {
        try {
            return getDouble(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * <p>
     * Get the {@link Integer} value associated with a key.
     * </p>
     *
     * @param key A key string.
     * @return The integer value, or <code>null</code> in case there is no value with specified key, or the value cannot
     * be parsed as Integer.
     * @throws JsonException
     */
    public int getInt(String key) throws JsonException {
        return JsonUtil.parseInt(this.get(key));
    }

    public Integer tryGetInt(String key) {
        try {
            return getInt(key);
        } catch (Exception e) {
            return null;
        }
    }

    public Integer tryGetInt(String key, Integer defaultValue) {
        try {
            return getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * <p>
     * Get the {@link JsonArray} value associated with a key.
     * </p>
     *
     * @param key A key string.
     * @return A JsonArray value, or <code>null</code> in case there is no value with specified key, or the value is no
     * {@link JsonArray}.
     * @throws JsonException
     */
    public JsonArray getJsonArray(String key) throws JsonException {
        return JsonUtil.parseJsonArray(this.get(key));
    }

    public JsonArray tryGetJsonArray(String key) {
        try {
            return getJsonArray(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * Get the {@link JsonObject} value associated with a key.
     * </p>
     *
     * @param key A key string.
     * @return A JsonObject value, or <code>null</code> in case there is no value with specified key, or the value is no
     * {@link JsonObject} .
     * @throws JsonException
     */
    public JsonObject getJsonObject(String key) throws JsonException {
        return JsonUtil.parseJsonObject(map.get(key));
    }

    public JsonObject tryGetJsonObject(String key) {
        try {
            return getJsonObject(key);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    /**
     * <p>
     * Get the {@link Long} value associated with a key.
     * </p>
     *
     * @param key A key string.
     * @return The long value, or <code>null</code> in case there is no value with specified key, or the value cannot be
     * parsed as Long.
     * @throws JsonException
     */
    public long getLong(String key) throws JsonException {
        return JsonUtil.parseLong(this.get(key));
    }

    public Long tryGetLong(String key) {
        try {
            return getLong(key);
        } catch (Exception e) {
            return null;
        }
    }

    public Long tryGetLong(String key, Long defaultValue) {
        try {
            return getLong(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * <p>
     * Get the {@link String} associated with a key.
     * </p>
     *
     * @param key A key string.
     * @return A string value, or <code>null</code> in case there is no value with specified key.
     * @throws JsonException
     */
    public String getString(String key) throws JsonException {
        if (containsKey(key)) {
            return JsonUtil.parseString(this.get(key));
        } else {
            throw new JsonException("No key: " + key);
        }
    }

    public String tryGetString(String key) {
        try {
            return getString(key);
        } catch (Exception e) {
            return null;
        }
    }

    public String tryGetString(String key, String defaultValue) {
        try {
            return getString(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    @Override
    public Object put(String key, Object value) {
        if (key == null) {
            throw new NullPointerException("Null key.");
        }
        try {
            JsonUtil.testValidity(value);
        } catch (JsonException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        return map.put(key, value);
    }

    /**
     * <p>
     * Make a JSON text of this JsonObject. For compactness, no whitespace is added. If this would not result in a
     * syntactically correct JSON text, then null will be returned instead. <b>Warning:</b> This method assumes that the
     * data structure is acyclical.
     * </p>
     *
     * @return a printable, displayable, portable, transmittable representation of the object, beginning with
     * <code>{</code>&nbsp;<small>(left brace)</small> and ending with <code>}</code>&nbsp;<small>(right
     * brace)</small>.
     */
    @Override
    public String toString() {
        try {
            return this.toString(0);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String toString(int indentFactor) {
        Config conf = JsoniterSpi.getCurrentConfig().copyBuilder().indentionStep(indentFactor).build();
        return JsonStream.serialize(conf, this);
    }

    @Override
    public boolean containsKey(Object key) {
        return map.containsKey(key);
    }

    @Override
    public Object query(String jPath) throws JsonException {
        if (jPath.isEmpty()) {
            return this;
        }
        return query(this, jPath);
    }

    private Object query(Object value, String jPath) throws JsonException {
        String[] pathSplit = JsonUtil.splitJPath(jPath);
        String key = pathSplit[0];

        Object value2 = null;
        if (value instanceof List) {
            List v = (List) value;
            int index = MathHelper.parseStringNumber(key).intValue();
            if (index >= v.size()) {
                throw new JsonException("Illegal index: " + index);
            }
            value2 = v.get(index);
        } else if (value instanceof Map) {
            Map v = (Map) value;
            if (!v.containsKey(key)) {
                throw new JsonException("No key: " + key);
            }
            value2 = v.get(key);
        }

        String remainingPath = pathSplit[1];
        if (remainingPath.isEmpty()) {
            return value2;
        }

        if (value2 instanceof Json) {
            Json child = (Json) value2;
            return child.query(remainingPath);
        } else {
            return query(value2, remainingPath.substring(1));
        }
    }

    public String tryQueryJsonPathString(String jPath) {
        try {
            return JsonUtil.parseString(queryJsonPath(jPath));
        } catch (Exception e) {
            return null;
        }
    }

    public Double tryQueryJsonPathDouble(String jPath) {
        try {
            return JsonUtil.parseDouble(queryJsonPath(jPath));
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean tryQueryJsonPathBoolean(String jPath) {
        try {
            return JsonUtil.parseBoolean(queryJsonPath(jPath));
        } catch (Exception e) {
            return null;
        }
    }

    public Integer tryQueryJsonPathInt(String jPath) {
        try {
            return JsonUtil.parseInt(queryJsonPath(jPath));
        } catch (Exception e) {
            return null;
        }
    }

    public JsonArray tryQueryJsonPathJsonArray(String jPath) {
        try {
            return JsonUtil.parseJsonArray(queryJsonPath(jPath));
        } catch (Exception e) {
            return null;
        }
    }

    public JsonObject tryQueryJsonPathJsonObject(String jPath) {
        try {
            return JsonUtil.parseJsonObject(queryJsonPath(jPath));
        } catch (Exception e) {
            return null;
        }
    }

    public Object queryJsonPath(String jPath) {
        net.minidev.json.JSONArray jsa = JsonPath.read(this, jPath);
        if (jsa == null || jsa.isEmpty()) {
            return null;
        }
        return jsa.get(0);
    }

    public List<Object> queryJsonPathArray(String jPath) {
        net.minidev.json.JSONArray jsa = JsonPath.read(this, jPath);
        if (jsa == null || jsa.isEmpty()) {
            return null;
        }
        return new ArrayList<>(jsa);
    }

    @Override
    public boolean queryBoolean(String jPath) throws JsonException {
        return JsonUtil.parseBoolean(query(jPath));
    }

    public Boolean tryQueryBoolean(String jPath) {
        try {
            return queryBoolean(jPath);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public double queryDouble(String jPath) throws JsonException {
        return JsonUtil.parseDouble(query(jPath));
    }

    public Double tryQueryDouble(String jPath) {
        try {
            return queryDouble(jPath);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int queryInt(String jPath) throws JsonException {
        return JsonUtil.parseInt(query(jPath));
    }

    public Integer tryQueryInt(String jPath) {
        try {
            return queryInt(jPath);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public JsonArray queryJsonArray(String jPath) throws JsonException {
        return JsonUtil.parseJsonArray(query(jPath));
    }

    public JsonArray tryQueryJsonArray(String jPath) {
        try {
            return queryJsonArray(jPath);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public JsonObject queryJsonObject(String jPath) throws JsonException {
        return JsonUtil.parseJsonObject(query(jPath));
    }

    public JsonObject tryQueryJsonObject(String jPath) {
        try {
            return queryJsonObject(jPath);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public long queryLong(String jPath) throws JsonException {
        return JsonUtil.parseLong(query(jPath));
    }

    public Long tryQueryLong(String jPath) {
        try {
            return queryLong(jPath);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public String queryString(String jPath) throws JsonException {
        return JsonUtil.parseString(query(jPath));
    }

    public String tryQueryString(String jPath) {
        try {
            return queryString(jPath);
        } catch (Exception e) {
            return null;
        }
    }
}
