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
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * <p>
 * A {@link JsonArray} is an ordered sequence of values. Its external text form is a string wrapped in square brackets
 * with commas separating the values. The array conforms to the {@link List} interface, allowing list manipulation,
 * iteration, etc. The typed getters (e.g. {@link #getDouble(int)}) allow retrieving values for the corresponding type,
 * in case, the type in the JSON is incompatible to the requested type, <code>null</code> is returned.
 * </p>
 * 
 * <p>
 * The constructor can convert a JSON text into a Java object. The {@link #toString()} method converts to JSON text. The
 * texts produced by the <code>toString</code> methods strictly conform to JSON syntax rules. The constructors are more
 * forgiving in the texts they will accept:
 * </p>
 * 
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just before the closing bracket.</li>
 * <li>The <code>null</code> value will be inserted when there is <code>,</code> &nbsp;<small>(comma)</small> elision.</li>
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
 * @version 2013-04-18
 */
public class JsonArray extends AbstractList<Object> implements Json {

    /** The arrayList where the JsonArray's properties are kept. */
    private final List<Object> list;

    /**
     * <p>
     * Construct an empty {@link JsonArray}.
     * </p>
     */
    public JsonArray() {
        this.list = new ArrayList<Object>();
    }

    JsonArray(JsonTokener x) throws JsonException {
        this();
        if (x.nextClean() != '[') {
            throw x.syntaxError("A JSON array text must start with '['");
        }
        if (x.nextClean() != ']') {
            x.back();
            for (;;) {
                if (x.nextClean() == ',') {
                    x.back();
                    list.add(null);
                } else {
                    x.back();
                    list.add(x.nextValue());
                }
                switch (x.nextClean()) {
                    case ',':
                        if (x.nextClean() == ']') {
                            return;
                        }
                        x.back();
                        break;
                    case ']':
                        return;
                    default:
                        throw x.syntaxError("Expected a ',' or ']'");
                }
            }
        }
    }

    /**
     * <p>
     * Construct a {@link JsonArray} from a source JSON text.
     * </p>
     * 
     * @param source A string that begins with <code>[</code>&nbsp;<small>(left bracket)</small> and ends with
     *            <code>]</code>&nbsp;<small>(right bracket)</small>.
     * @throws JsonException If there is a syntax error.
     */
    public JsonArray(String source) throws JsonException {
        this(new JsonTokener(source));
    }

    /**
     * <p>
     * Construct a {@link JsonArray} from a {@link Collection}.
     * </p>
     * 
     * @param collection A Collection.
     */
    public JsonArray(Collection<?> collection) {
        list = new ArrayList<Object>();
        if (collection != null) {
            Iterator<?> iter = collection.iterator();
            while (iter.hasNext()) {
                list.add(iter.next());
            }
        }
    }

    JsonArray(Object array) {
        this();
        if (array.getClass().isArray()) {
            int length = Array.getLength(array);
            for (int i = 0; i < length; i += 1) {
                this.add(Array.get(array, i));
            }
        } else {
            throw new IllegalArgumentException("JSON array initial value should be a string or collection or array.");
        }
    }

    /**
     * <p>
     * Get the {@link Object} value associated with an index.
     * </p>
     * 
     * @param index The index must be between 0 and length() - 1.
     * @return An object value, or <code>null</code> in case there is no value with specified index.
     * @throws IndexOutOfBoundsException If index is below zero or greater/equal its size.
     */
    @Override
    public Object get(int index) {
        // return index < 0 || index >= this.size() ? null : list.get(index);
        return list.get(index);
    }

    /**
     * <p>
     * Get the {@link Boolean} value associated with an index. The string values "true" and "false" are converted to
     * boolean.
     * </p>
     * 
     * @param index The index must be between 0 and length() - 1.
     * @return The boolean value, or <code>null</code> in case there is no value with specified index, or the value
     *         cannot be parsed as boolean.
     * @throws JsonException
     */
    public boolean getBoolean(int index) throws JsonException {
        return JsonUtil.parseBoolean(this.get(index));
    }

    public Boolean tryGetBoolean(int index) {
        try {
            return getBoolean(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * Get the {@link Double} value associated with an index.
     * </p>
     * 
     * @param index The index must be between 0 and length() - 1.
     * @return The double value, or <code>null</code> in case there is no value with specified index, or the value
     *         cannot be parsed as Double.
     * @throws JsonException
     */
    public double getDouble(int index) throws JsonException {
        return JsonUtil.parseDouble(this.get(index));
    }

    public Double tryGetDouble(int index) {
        try {
            return getDouble(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * Get the {@link Integer} value associated with an index.
     * </p>
     * 
     * @param index The index must be between 0 and length() - 1.
     * @return The integer value, or <code>null</code> in case there is no value with specified index, or the value
     *         cannot be parsed as Integer.
     * @throws JsonException
     */
    public int getInt(int index) throws JsonException {
        return JsonUtil.parseInt(this.get(index));
    }

    public Integer tryGetInt(int index) {
        try {
            return getInt(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * Get the {@link JsonArray} associated with an index.
     * </p>
     * 
     * @param index The index must be between 0 and length() - 1.
     * @return A JsonArray value, or <code>null</code> in case there is no value with specified index, or the value is
     *         no {@link JsonArray}.
     * @throws JsonException
     */
    public JsonArray getJsonArray(int index) throws JsonException {
        return JsonUtil.parseJSONArray(this.get(index));
    }

    public JsonArray tryGetJsonArray(int index) {
        try {
            return getJsonArray(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * Get the {@link JsonObject} associated with an index.
     * </p>
     * 
     * @param index The index must be between 0 and length() - 1.
     * @return A JsonObject value, or <code>null</code> in case there is no value with specified index, or the value is
     *         no {@link JsonObject}.
     * @throws JsonException
     */
    public JsonObject getJsonObject(int index) throws JsonException {
        return JsonUtil.parseJSONObject(this.get(index));
    }

    public JsonObject tryGetJsonObject(int index) {
        try {
            return getJsonObject(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * Get the {@link Long} value associated with an index.
     * </p>
     * 
     * @param index The index must be between 0 and length() - 1.
     * @return The long value, or <code>null</code> in case there is no value with specified index, or the value cannot
     *         be parsed as Long.
     * @throws JsonException
     */
    public long getLong(int index) throws JsonException {
        return JsonUtil.parseLong(this.get(index));
    }

    public Long tryGetLong(int index) {
        try {
            return getLong(index);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * Get the {@link String} associated with an index.
     * </p>
     * 
     * @param index The index must be between 0 and length() - 1.
     * @return A string value, or <code>null</code> in case there is no value with specified index.
     * @throws JsonException
     */
    public String getString(int index) throws JsonException {
        return JsonUtil.parseString(this.get(index));
    }

    public String tryGetString(int index) {
        try {
            return getString(index);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public Object set(int index, Object element) {
        try {
            JsonUtil.testValidity(element);
        } catch (JsonException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        if (index < 0) {
            throw new IllegalArgumentException("JsonArray[" + index + "] not found.");
        }
        if (index < this.size()) {
            return list.set(index, element);
        } else {
            while (index != this.size()) {
                list.add(null);
            }
            list.add(element);
            return null;
        }
    }

    @Override
    public void add(int index, Object element) {
        try {
            JsonUtil.testValidity(element);
        } catch (JsonException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        list.add(element);
    }

    @Override
    public Object remove(int index) {
        return list.remove(index);
    }

    /**
     * <p>
     * Make a JSON text of this JsonArray. For compactness, no unnecessary whitespace is added. If it is not possible to
     * produce a syntactically correct JSON text then null will be returned instead. This could occur if the array
     * contains an invalid number. <b>Warning:</b> This method assumes that the data structure is acyclical.
     * </p>
     * 
     * @return a printable, displayable, transmittable representation of the array.
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
        int length = this.size();
        writer.write('[');

        if (length == 1) {
            JsonUtil.writeValue(writer, list.get(0), indentFactor, indent);
        } else if (length != 0) {
            final int newindent = indent + indentFactor;

            for (int i = 0; i < length; i += 1) {
                if (commanate) {
                    writer.write(',');
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                JsonUtil.indent(writer, newindent);
                JsonUtil.writeValue(writer, list.get(i), indentFactor, newindent);
                commanate = true;
            }
            if (indentFactor > 0) {
                writer.write('\n');
            }
            JsonUtil.indent(writer, indent);
        }
        writer.write(']');
        return writer;
    }

    @Override
    public Object query(String jPath) throws JsonException {
        if (jPath.isEmpty()) {
            return this;
        }
        String[] pathSplit = JsonUtil.splitJPath(jPath);
        String head = pathSplit[0];
        String remainingPath = pathSplit[1];
        if (!head.matches("\\[\\d+\\]")) {
            return null;
        }
        int index = Integer.valueOf(head.substring(1, head.length() - 1));
        Object value;
        try {
            value = get(index);
        } catch (IndexOutOfBoundsException e) {
            throw new JsonException("Illegal index: " + index);
        }
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
