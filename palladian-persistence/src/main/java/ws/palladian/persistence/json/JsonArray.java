package ws.palladian.persistence.json;

import com.jsoniter.JsonIterator;
import com.jsoniter.any.Any;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.Config;
import com.jsoniter.spi.JsoniterSpi;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import ws.palladian.helper.nlp.PatternHelper;

import java.io.IOException;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.lang.reflect.Array;
import java.util.AbstractList;
import java.util.Collection;
import java.util.Map;

/**
 * @author Philipp Katz, David Urbansky
 * @version 2023-01-16
 */
public class JsonArray extends AbstractList<Object> implements Json, Serializable {
    static {
        JsoniterSpi.registerTypeDecoder(Object.class, iter -> {
            Object read = iter.read();
            if (read == null) {
                return null;
            }

            if (read instanceof Map) {
                return new JsonObject((Map<String, Object>) read);
            } else if (read instanceof Collection) {
                return new JsonArray(read);
            }

            return read;
        });
    }

    /** The arrayList where the JsonArray's properties are kept. */
    private ObjectArrayList<Object> list;

    /**
     * <p>
     * Construct an empty {@link JsonArray}.
     * </p>
     */
    public JsonArray() {
        this.list = new ObjectArrayList<>();
    }

    /**
     * <p>
     * Try to construct a {@link JsonArray} from a source JSON text string. Instead of the constructor, this method
     * does not throw a {@link JsonException} in case the JsonArray cannot be constructed.
     * </p>
     *
     * @param source A string beginning with <code>[</code>&nbsp;<small>(left bracket)</small> and ending with
     *               <code>]</code>&nbsp;<small>(right bracket)</small>.
     * @return The {@link JsonArray}, or <code>null</code> in case it could not be parsed.
     */
    public static JsonArray tryParse(String source) {
        if (source == null) {
            return null;
        }
        try {
            return new JsonArray(source);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * <p>
     * Construct a {@link JsonArray} from a source JSON text.
     * </p>
     *
     * @param source A string that begins with <code>[</code>&nbsp;<small>(left bracket)</small> and ends with
     *               <code>]</code>&nbsp;<small>(right bracket)</small>.
     * @throws JsonException If there is a syntax error.
     */
    public JsonArray(String source) throws JsonException {
        if (source == null || source.isEmpty()) {
            list = new ObjectArrayList<>();
            return;
        }
        Any any;
        try {
            any = JsonIterator.deserialize(source);
            list = any.as(ObjectArrayList.class);
        } catch (Exception e) {
            // remove trailing commas
            source = PatternHelper.compileOrGet(",\\s*(?=[}\\]])").matcher(source).replaceAll("");
            try {
                any = JsonIterator.deserialize(source);
                list = any.as(ObjectArrayList.class);
            } catch (Exception e2) {
                parseFallback(new JsonTokener(source));
            }
        }
        if (list == null) {
            list = new ObjectArrayList<>();
        }
    }

    private void parseFallback(JsonTokener x) throws JsonException {
        list = new ObjectArrayList<>();
        if (x.nextClean() != '[') {
            throw x.syntaxError("A JSON array text must start with '['");
        }
        if (x.nextClean() != ']') {
            x.back();
            for (; ; ) {
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
     * Construct a {@link JsonArray} from a {@link Collection}.
     * </p>
     *
     * @param collection A Collection.
     */
    public JsonArray(Collection<?> collection) {
        list = new ObjectArrayList<>();
        if (collection != null) {
            for (Object value : collection) {
                this.add(value);
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
        } else if (array instanceof Collection) {
            for (Object o : (Collection) array) {
                this.add(o);
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
        Object value = list.get(index);
        try {
            if (value instanceof Json || value instanceof Map || value instanceof Collection) {
                return JsonUtils.parseJsonObjectOrArray(value);
            }
            return value;
        } catch (JsonException e) {
            return value;
        }
    }

    @Override
    public boolean add(Object value) {
        if (value instanceof Json) {
            return this.list.add(value);
        } else if (value instanceof Map) {
            return this.list.add(new JsonObject((Map<String, Object>) value));
        } else if (value instanceof Collection) {
            return this.list.add(new JsonArray(value));
        } else {
            return this.list.add(value);
        }
    }

    /**
     * <p>
     * Get the {@link Boolean} value associated with an index. The string values "true" and "false" are converted to
     * boolean.
     * </p>
     *
     * @param index The index must be between 0 and length() - 1.
     * @return The boolean value, or <code>null</code> in case there is no value with specified index, or the value
     * cannot be parsed as boolean.
     */
    public boolean getBoolean(int index) throws JsonException {
        return JsonUtils.parseBoolean(this.get(index));
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
     * cannot be parsed as Double.
     */
    public double getDouble(int index) throws JsonException {
        return JsonUtils.parseDouble(this.get(index));
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
     * cannot be parsed as Integer.
     * @throws JsonException
     */
    public int getInt(int index) throws JsonException {
        return JsonUtils.parseInt(this.get(index));
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
     * no {@link JsonArray}.
     */
    public JsonArray getJsonArray(int index) throws JsonException {
        return JsonUtils.parseJsonArray(this.get(index));
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
     * no {@link JsonObject}.
     */
    public JsonObject getJsonObject(int index) throws JsonException {
        return JsonUtils.parseJsonObject(this.get(index));
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
     * be parsed as Long.
     */
    public long getLong(int index) throws JsonException {
        return JsonUtils.parseLong(this.get(index));
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
     */
    public String getString(int index) throws JsonException {
        return JsonUtils.parseString(this.get(index));
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
            JsonUtils.testValidity(element);
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
            JsonUtils.testValidity(element);
        } catch (JsonException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
        list.add(index, element);
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
            Config conf = JsoniterSpi.getCurrentConfig().copyBuilder().indentionStep(indentFactor).build();
            return JsonStream.serialize(conf, this);
        } catch (Exception e) {
            try {
                return this.write(new StringWriter(), indentFactor, 0).toString();
            } catch (IOException ex) {
                return null;
            }
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
            JsonUtils.writeValue(writer, list.get(0), indentFactor, indent);
        } else if (length != 0) {
            final int newindent = indent + indentFactor;

            for (int i = 0; i < length; i += 1) {
                if (commanate) {
                    writer.write(',');
                }
                if (indentFactor > 0) {
                    writer.write('\n');
                }
                JsonUtils.indent(writer, newindent);
                JsonUtils.writeValue(writer, list.get(i), indentFactor, newindent);
                commanate = true;
            }
            if (indentFactor > 0) {
                writer.write('\n');
            }
            JsonUtils.indent(writer, indent);
        }
        writer.write(']');
        return writer;
    }

    @Override
    public Object query(String jPath) throws JsonException {
        if (jPath.isEmpty()) {
            return this;
        }
        String[] pathSplit = JsonUtils.splitJPath(jPath);
        String head = pathSplit[0];
        String remainingPath = pathSplit[1];
        if (!head.matches("\\[\\d+]")) {
            return null;
        }
        int index = Integer.parseInt(head.substring(1, head.length() - 1));
        Object value;
        try {
            value = get(index);
        } catch (IndexOutOfBoundsException e) {
            throw new JsonException("Illegal index: " + index);
        }
        if (value instanceof Json) {
            Json child = (Json) value;
            return child.query(remainingPath);
        } else if (remainingPath.isEmpty()) {
            return value;
        } else {
            throw new JsonException("No value/item for query.");
        }
    }

    @Override
    public boolean queryBoolean(String jPath) throws JsonException {
        return JsonUtils.parseBoolean(query(jPath));
    }

    @Override
    public double queryDouble(String jPath) throws JsonException {
        return JsonUtils.parseDouble(query(jPath));
    }

    @Override
    public int queryInt(String jPath) throws JsonException {
        return JsonUtils.parseInt(query(jPath));
    }

    @Override
    public JsonArray queryJsonArray(String jPath) throws JsonException {
        return JsonUtils.parseJsonArray(query(jPath));
    }

    @Override
    public JsonObject queryJsonObject(String jPath) throws JsonException {
        return JsonUtils.parseJsonObject(query(jPath));
    }

    @Override
    public long queryLong(String jPath) throws JsonException {
        return JsonUtils.parseLong(query(jPath));
    }

    @Override
    public String queryString(String jPath) throws JsonException {
        return JsonUtils.parseString(query(jPath));
    }

    public static void main(String[] args) throws IOException {
        String jsonString = "{\"id\":5555,\"ar\":[\"sadfs\",\"222\"],\"jso\":{\"i\":123}}";

        //        Any any = JsonIterator.deserialize(jsonString);
        //        Object2ObjectLinkedOpenHashMap as = any.as(Object2ObjectLinkedOpenHashMap.class);
        //        System.out.println(as);
        //        JsonObject jsonObject1 = JsonObject.tryParse(jsonString);
        //        System.out.println(jsonObject1);
        //        System.exit(0);
        //
        //        JsonIterator iter = JsonIterator.parse(jsonString);
        //        JsonObject read = iter.read(JsonObject.class);
        //        System.out.println(read);
        //        System.exit(0);

        jsonString = "[{\"id\":5555,\"text\":\"sadfs\",\"ar\":[\"sadfs\",\"222\"],\"jso\":{\"i\":123}}]";
        JsonArray jsonArray = JsonArray.tryParse(jsonString);
        for (int i = 0; i < jsonArray.size(); i++) {
            JsonObject jsonObject = jsonArray.tryGetJsonObject(i);
            jsonObject.put("name", "lala");
        }
        System.out.println(jsonArray);
        System.exit(0);
    }
}
