package ws.palladian.persistence.json;

import com.dslplatform.json.DslJson;
import com.dslplatform.json.runtime.TypeDefinition;
import com.jayway.jsonpath.JsonPath;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.PatternHelper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

/**
 * @author Philipp Katz, David Urbansky
 * @version 2023-01-16
 */
@SuppressWarnings("serial")
public class JsonObject extends AbstractMap<String, Object> implements Json, Jsonable, Serializable {

    // DSL-JSON instance for (de)serialization
    private static final DslJson<Object> DSL = new DslJson<>();
    public static final Pattern CLEANING_PATTERN = PatternHelper.compileOrGet(",\\s*(?=[}\\]])");

    /** The map where the JsonObject's properties are kept. */
    private Object2ObjectMap<String, Object> map;

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
    public JsonObject(Map<?, ?> map) {
        this.map = new Object2ObjectLinkedOpenHashMap<>();
        if (map != null) {
            for (Object key : map.keySet()) {
                Object value = map.get(key);
                if (value != null) {
                    if (value instanceof Map mapValue) {
                        this.map.put(key.toString(), new JsonObject(mapValue));
                    } else if (value instanceof Collection collection) {
                        this.map.put(key.toString(), new JsonArray(collection));
                    } else {
                        this.map.put(key.toString(), value);
                    }
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
        if (source == null) {
            return null;
        }
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
    @SuppressWarnings("unchecked")
    public JsonObject(String source) throws JsonException {
        if (source == null || source.isEmpty()) {
            map = new Object2ObjectLinkedOpenHashMap<>();
            return;
        }
        try {
            byte[] bytes = source.getBytes(StandardCharsets.UTF_8);
            Map<String, Object> parsed = (Map<String, Object>) DSL.deserialize(new TypeDefinition<Map<String, Object>>() {
            }.type, bytes, bytes.length);
            if (parsed != null) {
                map = new Object2ObjectLinkedOpenHashMap<>();
                for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                    Object value = entry.getValue();
                    if (value instanceof Map<?, ?> m) {
                        map.put(entry.getKey(), new JsonObject(m));
                    } else if (value instanceof Collection<?> c) {
                        map.put(entry.getKey(), new JsonArray(c));
                    } else {
                        map.put(entry.getKey(), value);
                    }
                }
            } else {
                String cleaned = CLEANING_PATTERN.matcher(source).replaceAll("");
                parseFallback(new JsonTokener(cleaned));
            }
        } catch (Exception e) {
            String cleaned = CLEANING_PATTERN.matcher(source).replaceAll("");
            try {
                byte[] bytes = cleaned.getBytes(StandardCharsets.UTF_8);
                Map<String, Object> parsed = (Map<String, Object>) DSL.deserialize(new TypeDefinition<Map<String, Object>>() {
                }.type, bytes, bytes.length);
                if (parsed != null) {
                    map = new Object2ObjectLinkedOpenHashMap<>();
                    for (Map.Entry<String, Object> entry : parsed.entrySet()) {
                        Object value = entry.getValue();
                        if (value instanceof Map<?, ?> m) {
                            map.put(entry.getKey(), new JsonObject(m));
                        } else if (value instanceof Collection<?> c) {
                            map.put(entry.getKey(), new JsonArray(c));
                        } else {
                            map.put(entry.getKey(), value);
                        }
                    }
                } else {
                    parseFallback(new JsonTokener(cleaned));
                }
            } catch (Exception e2) {
                parseFallback(new JsonTokener(cleaned));
            }
        }
        if (map == null) {
            map = new Object2ObjectLinkedOpenHashMap<>();
        }
    }

    JsonObject(JsonTokener x) throws JsonException {
        this();
        parseFallback(x);
    }

    private void parseFallback(JsonTokener x) throws JsonException {
        map = new Object2ObjectLinkedOpenHashMap<>();
        char c;
        String key;

        if (x.nextClean() != '{') {
            throw x.syntaxError("A JSON object text must begin with '{'");
        }
        for (; ; ) {
            c = x.nextClean();
            switch (c) {
                case 0:
                    throw x.syntaxError("A JSON object text must end with '}'");
                case '}':
                    return;
                default:
                    x.back();
                    key = x.nextValue().toString();
            }

            // The key is followed by ':'.

            c = x.nextClean();
            if (c != ':') {
                throw x.syntaxError("Expected a ':' after a key");
            }
            this.put(key, x.nextValue());

            // Pairs are separated by ','.

            switch (x.nextClean()) {
                case ';':
                case ',':
                    if (x.nextClean() == '}') {
                        return;
                    }
                    x.back();
                    break;
                case '}':
                    return;
                default:
                    throw x.syntaxError("Expected a ',' or '}'");
            }
        }
    }

    @Override
    public Writer write(Writer writer) throws IOException {
        return this.write(writer, 0, 0);
    }

    /** Fallback writer if Jsoniter fails */
    Writer write(Writer writer, int indentFactor, int indent) throws IOException {
        boolean commanate = false;
        final int length = this.size();
        Iterator<String> keys = this.keySet().iterator();
        writer.write('{');

        if (length == 1) {
            Object key = keys.next();
            writer.write(JsonUtils.quote(key.toString()));
            writer.write(':');
            if (indentFactor > 0) {
                writer.write(' ');
            }
            JsonUtils.writeValue(writer, map.get(key), indentFactor, indent);
        } else if (length != 0) {
            final int newindent = indent + indentFactor;
            while (keys.hasNext()) {
                Object key = keys.next();
                if (commanate) {
                    writer.write(',');
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                JsonUtils.indent(writer, newindent);
                writer.write(JsonUtils.quote(key.toString()));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                JsonUtils.writeValue(writer, map.get(key), indentFactor, newindent);
                commanate = true;
            }
            if (indentFactor > 0) {
                writer.write('\n');
            }
            JsonUtils.indent(writer, indent);
        }
        writer.write('}');
        return writer;
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
        if (key == null) {
            return null;
        }
        Object value = map.get(key);
        try {
            if (value instanceof Json || value instanceof Map || value instanceof Collection) {
                return JsonUtils.parseJsonObjectOrArray(value);
            }
            return value;
        } catch (JsonException e) {
            return value;
        }
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
        return JsonUtils.parseBoolean(this.get(key));
    }

    public Boolean tryGetBoolean(String key) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return null;
            } else {
                return JsonUtils.parseBoolean(o);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean tryGetBoolean(String key, Boolean defaultValue) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return defaultValue;
            } else {
                return JsonUtils.parseBoolean(o);
            }
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
        return JsonUtils.parseDouble(this.get(key));
    }

    public Double tryGetDouble(String key) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return null;
            } else {
                return JsonUtils.parseDouble(o);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Double tryGetDouble(String key, Double defaultValue) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return defaultValue;
            } else {
                return JsonUtils.parseDouble(o);
            }
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
        return JsonUtils.parseInt(this.get(key));
    }

    public Integer tryGetInt(String key) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return null;
            } else {
                return JsonUtils.parseInt(o);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Integer tryGetInt(String key, Integer defaultValue) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return defaultValue;
            } else {
                return JsonUtils.parseInt(o);
            }
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
        return JsonUtils.parseJsonArray(this.get(key));
    }

    public JsonArray tryGetJsonArray(String key) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return null;
            } else {
                return JsonUtils.parseJsonArray(o);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public JsonArray tryGetJsonArray(String key, JsonArray defaultValue) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return defaultValue;
            } else {
                return JsonUtils.parseJsonArray(o);
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }

    /**
     * Get the {@link JsonObject} value associated with a key.
     *
     * @param key A key string.
     * @return A JsonObject value, or <code>null</code> in case there is no value with specified key, or the value is no
     * {@link JsonObject} .
     * @throws JsonException
     */
    public JsonObject getJsonObject(String key) throws JsonException {
        return JsonUtils.parseJsonObject(this.get(key));
    }

    public JsonObject tryGetJsonObject(String key) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return null;
            } else {
                return JsonUtils.parseJsonObject(o);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public JsonObject tryGetJsonObject(String key, JsonObject defaultValue) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return defaultValue;
            } else {
                return JsonUtils.parseJsonObject(o);
            }
        } catch (Exception e) {
            return defaultValue;
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
        return JsonUtils.parseLong(this.get(key));
    }

    public Long tryGetLong(String key) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return null;
            } else {
                return JsonUtils.parseLong(o);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Long tryGetLong(String key, Long defaultValue) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return defaultValue;
            } else {
                return JsonUtils.parseLong(o);
            }
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
            return JsonUtils.parseString(this.get(key));
        } else {
            throw new JsonException("No key: " + key);
        }
    }

    public String tryGetString(String key) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return null;
            } else {
                return JsonUtils.parseString(o);
            }
        } catch (Exception e) {
            return null;
        }
    }

    public String tryGetString(String key, String defaultValue) {
        try {
            Object o = this.get(key);
            if (o == null) {
                return defaultValue;
            } else {
                return JsonUtils.parseString(o);
            }
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
            JsonUtils.testValidity(value);
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
        return this.toString(0);
    }

    @Override
    public String toString(int indentFactor) {
        // Pretty printing via fallback writer, compact via DSL-JSON
        if (indentFactor > 0) {
            try {
                return this.write(new StringWriter(), indentFactor, 0).toString();
            } catch (IOException ex) {
                return null;
            }
        }
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            DSL.serialize(this.map, baos);
            return new String(baos.toByteArray(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            try {
                return this.write(new StringWriter(), 0, 0).toString();
            } catch (IOException ex) {
                return null;
            }
        }
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
        String[] pathSplit = JsonUtils.splitJPath(jPath);
        String key = pathSplit[0];

        Object value2 = null;
        if (value instanceof List v) {
            int index = MathHelper.parseStringNumber(key).intValue();
            if (index >= v.size()) {
                throw new JsonException("Illegal index: " + index);
            }
            value2 = v.get(index);
        } else if (value instanceof Map v) {
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
            return JsonUtils.parseString(queryJsonPath(jPath));
        } catch (Exception e) {
            return null;
        }
    }

    public Double tryQueryJsonPathDouble(String jPath) {
        try {
            return JsonUtils.parseDouble(queryJsonPath(jPath));
        } catch (Exception e) {
            return null;
        }
    }

    public Boolean tryQueryJsonPathBoolean(String jPath) {
        try {
            return JsonUtils.parseBoolean(queryJsonPath(jPath));
        } catch (Exception e) {
            return null;
        }
    }

    public Integer tryQueryJsonPathInt(String jPath) {
        try {
            return JsonUtils.parseInt(queryJsonPath(jPath));
        } catch (Exception e) {
            return null;
        }
    }

    public JsonArray tryQueryJsonPathJsonArray(String jPath) {
        try {
            return JsonUtils.parseJsonArray(queryJsonPath(jPath));
        } catch (Exception e) {
            return null;
        }
    }

    public JsonObject tryQueryJsonPathJsonObject(String jPath) {
        try {
            return JsonUtils.parseJsonObject(queryJsonPath(jPath));
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
        return JsonUtils.parseBoolean(query(jPath));
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
        return JsonUtils.parseDouble(query(jPath));
    }

    public Double tryQueryDouble(String jPath, Double defaultValue) {
        try {
            return queryDouble(jPath);
        } catch (Exception e) {
            return defaultValue;
        }
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
        return JsonUtils.parseInt(query(jPath));
    }

    public Integer tryQueryInt(String jPath, Integer defaultValue) {
        try {
            return queryInt(jPath);
        } catch (Exception e) {
            return defaultValue;
        }
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
        return JsonUtils.parseJsonArray(query(jPath));
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
        return JsonUtils.parseJsonObject(query(jPath));
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
        return JsonUtils.parseLong(query(jPath));
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
        return JsonUtils.parseString(query(jPath));
    }

    public String tryQueryString(String jPath) {
        try {
            return queryString(jPath);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Remove null keys and null values
     */
    public void removeNulls() {
        remove(null);
        map.keySet().removeIf(key -> map.get(key) == null);
    }

    @Override
    public JsonObject asJson() {
        return this;
    }
}
