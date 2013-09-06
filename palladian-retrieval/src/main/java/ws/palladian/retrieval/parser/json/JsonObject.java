package ws.palladian.retrieval.parser.json;

/*
 * Copyright (c) 2002 JSON.org
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 * The Software shall be used for Good, not Evil.
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * <p>
 * A {@link JsonObject} is an unordered collection of name/value pairs. Its external form is a string wrapped in curly
 * braces with colons between the names and values, and commas between the values and names. The object conforms to the
 * {@link Map} interface, allowing map manipulation, iteration, lookup, etc. The typed getters (e.g.
 * {@link #getDouble(int)}) allow retrieving values for the corresponding type, in case, the type in the JSON is
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
public class JsonObject extends AbstractMap<String, Object> implements Json {

    /** The map where the JsonObject's properties are kept. */
    private final Map<String, Object> map;

    /**
     * <p>
     * Construct an empty {@link JsonObject}.
     * </p>
     */
    public JsonObject() {
        map = new HashMap<String, Object>();
    }

    JsonObject(JsonTokener x) throws JsonException {
        this();
        char c;
        String key;

        if (x.nextClean() != '{') {
            throw x.syntaxError("A JSON object text must begin with '{'");
        }
        for (;;) {
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

    /**
     * <p>
     * Construct a {@link JsonObject} from a {@link Map}.
     * </p>
     * 
     * @param map A map object that can be used to initialize the contents of the JsonObject.
     * @throws JsonException
     */
    public JsonObject(Map<?, ?> map) {
        this.map = new HashMap<String, Object>();
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
     * Construct a {@link JsonObject} from a source JSON text string. This is the most commonly used JsonObject
     * constructor.
     * </p>
     * 
     * @param source A string beginning with <code>{</code>&nbsp;<small>(left brace)</small> and ending with
     *            <code>}</code>&nbsp;<small>(right brace)</small>.
     * @exception JsonException If there is a syntax error in the source string or a duplicated key.
     */
    public JsonObject(String source) throws JsonException {
        this(new JsonTokener(source));
    }

    /**
     * <p>
     * Try to construct a {@link JsonObject} from a source JSON text string. Instead of the constructor, this method
     * does not throw a {@link JsonException} in case the JsonObject cannot be constructed.
     * </p>
     * 
     * @param source A string beginning with <code>{</code>&nbsp;<small>(left brace)</small> and ending with
     *            <code>}</code>&nbsp;<small>(right brace)</small>.
     * @return The {@link JsonObject}, or <code>null</code> in case it could not be parsed.
     */
    public static JsonObject tryParse(String source) {
        try {
            return new JsonObject(source);
        } catch (JsonException e) {
            return null;
        }
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
     *         be parsed as boolean.
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

    /**
     * <p>
     * Get the {@link Double} value associated with a key.
     * </p>
     * 
     * @param key A key string.
     * @return The double value, or <code>null</code> in case there is no value with specified key, or the value cannot
     *         be parsed as Double.
     * @throws JsonException
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

    /**
     * <p>
     * Get the {@link Integer} value associated with a key.
     * </p>
     * 
     * @param key A key string.
     * @return The integer value, or <code>null</code> in case there is no value with specified key, or the value cannot
     *         be parsed as Integer.
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

    /**
     * <p>
     * Get the {@link JsonArray} value associated with a key.
     * </p>
     * 
     * @param key A key string.
     * @return A JsonArray value, or <code>null</code> in case there is no value with specified key, or the value is no
     *         {@link JsonArray}.
     * @throws JsonException
     */
    public JsonArray getJsonArray(String key) throws JsonException {
        return JsonUtil.parseJSONArray(this.get(key));
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
     *         {@link JsonObject} .
     * @throws JsonException
     */
    public JsonObject getJsonObject(String key) throws JsonException {
        return JsonUtil.parseJSONObject(this.get(key));
    }

    public JsonObject tryGetJsonObject(String key) {
        try {
            return getJsonObject(key);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * Get the {@link Long} value associated with a key.
     * </p>
     * 
     * @param key A key string.
     * @return The long value, or <code>null</code> in case there is no value with specified key, or the value cannot be
     *         parsed as Long.
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
        return JsonUtil.parseString(this.get(key));
    }

    public String tryGetString(String key) {
        try {
            return getString(key);
        } catch (Exception e) {
            return null;
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
     *         <code>{</code>&nbsp;<small>(left brace)</small> and ending with <code>}</code>&nbsp;<small>(right
     *         brace)</small>.
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
        try {
            return this.write(new StringWriter(), indentFactor, 0).toString();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public Writer write(Writer writer) throws IOException {
        return this.write(writer, 0, 0);
    }

    Writer write(Writer writer, int indentFactor, int indent) throws IOException {
        boolean commanate = false;
        final int length = this.size();
        Iterator<String> keys = this.keySet().iterator();
        writer.write('{');

        if (length == 1) {
            Object key = keys.next();
            writer.write(JsonUtil.quote(key.toString()));
            writer.write(':');
            if (indentFactor > 0) {
                writer.write(' ');
            }
            JsonUtil.writeValue(writer, map.get(key), indentFactor, indent);
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
                JsonUtil.indent(writer, newindent);
                writer.write(JsonUtil.quote(key.toString()));
                writer.write(':');
                if (indentFactor > 0) {
                    writer.write(' ');
                }
                JsonUtil.writeValue(writer, map.get(key), indentFactor, newindent);
                commanate = true;
            }
            if (indentFactor > 0) {
                writer.write('\n');
            }
            JsonUtil.indent(writer, indent);
        }
        writer.write('}');
        return writer;
    }

    @Override
    public Set<Entry<String, Object>> entrySet() {
        return map.entrySet();
    }

    @Override
    public Object query(String jPath) throws JsonException {
        if (jPath.isEmpty()) {
            return this;
        }
        String[] pathSplit = JsonUtil.splitJPath(jPath);
        String key = pathSplit[0];
        if (!containsKey(key)) {
            throw new JsonException("No key: " + key);
        }
        Object value = get(key);
        String remainingPath = pathSplit[1];
        if (value instanceof Json) {
            Json child = (Json)value;
            return child.query(remainingPath);
        } else if (remainingPath.isEmpty()) {
            return value;
        } else {
            throw new JsonException("No value/item for query.");
        }
    }

    @Override
    public boolean queryBoolean(String jPath) throws JsonException {
        return JsonUtil.parseBoolean(query(jPath));
    }

    @Override
    public double queryDouble(String jPath) throws JsonException {
        return JsonUtil.parseDouble(query(jPath));
    }

    @Override
    public int queryInt(String jPath) throws JsonException {
        return JsonUtil.parseInt(query(jPath));
    }

    @Override
    public JsonArray queryJsonArray(String jPath) throws JsonException {
        return JsonUtil.parseJSONArray(query(jPath));
    }

    @Override
    public JsonObject queryJsonObject(String jPath) throws JsonException {
        return JsonUtil.parseJSONObject(query(jPath));
    }

    @Override
    public long queryLong(String jPath) throws JsonException {
        return JsonUtil.parseLong(query(jPath));
    }

    @Override
    public String queryString(String jPath) throws JsonException {
        return JsonUtil.parseString(query(jPath));
    }

}
