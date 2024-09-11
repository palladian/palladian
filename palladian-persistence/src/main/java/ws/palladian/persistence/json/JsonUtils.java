package ws.palladian.persistence.json;

import ws.palladian.helper.nlp.PatternHelper;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.*;
import java.util.regex.Pattern;

public class JsonUtils {
    private JsonUtils() {
        // util.
    }

    public static boolean parseBoolean(Object object) throws JsonException {
        try {
            if (object.equals(Boolean.FALSE) || object instanceof String && ((String) object).equalsIgnoreCase("false")) {
                return false;
            } else if (object.equals(Boolean.TRUE) || object instanceof String && ((String) object).equalsIgnoreCase("true")) {
                return true;
            }
        } catch (Exception e) {
        }
        throw new JsonException("Could not parse \"" + object + "\" to boolean.");
    }

    public static double parseDouble(Object object) throws JsonException {
        try {
            return object instanceof Number ? ((Number) object).doubleValue() : Double.parseDouble((String) object);
        } catch (Exception e) {
            throw new JsonException("Could not parse \"" + object + "\" to double.");
        }
    }

    public static int parseInt(Object object) throws JsonException {
        try {
            return object instanceof Number ? ((Number) object).intValue() : Integer.parseInt((String) object);
        } catch (Exception e) {
            throw new JsonException("Could not parse \"" + object + "\" to int.");
        }
    }

    public static JsonArray parseJsonArray(Object object) throws JsonException {
        if (object == null || object instanceof JsonArray) {
            return (JsonArray) object;
        } else if (object instanceof Collection) {
            return new JsonArray((Collection<?>) object);
        }
        throw new JsonException("Could not parse \"" + object + "\" to JSON array.");
    }

    public static JsonObject parseJsonObject(Object object) throws JsonException {
        if (object == null || object instanceof JsonObject) {
            return (JsonObject) object;
        }
        throw new JsonException("Could not parse \"" + object + "\" to JSON object.");
    }

    public static Long parseLong(Object object) throws JsonException {
        try {
            return object instanceof Number ? ((Number) object).longValue() : Long.parseLong((String) object);
        } catch (Exception e) {
            throw new JsonException("Could not parse \"" + object + "\" to long.");
        }
    }

    public static String parseString(Object object) throws JsonException {
        if (object instanceof Map || object instanceof List) {
            throw new JsonException("Could not parse \"" + object + "\" to string.");
        }
        if (object == null) {
            return null;
        }
        return object.toString();
    }

    public static Json parseJsonObjectOrArray(Object object) throws JsonException {
        if (object instanceof Json) {
            return (Json) object;
        }
        if (object instanceof Map) {
            return parseJsonObject(object);
        }
        if (object instanceof Collection) {
            return parseJsonArray(object);
        }
        throw new JsonException("Could not parse \"" + object + "\" to JSON.");
    }

    /**
     * Throw an exception if the object is a NaN or infinite number.
     *
     * @param o The object to test.
     * @throws JsonException If o is a non-finite number.
     */
    public static void testValidity(Object o) throws JsonException {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double) o).isInfinite() || ((Double) o).isNaN()) {
                    throw new JsonException("JSON does not allow non-finite numbers.");
                }
            } else if (o instanceof Float) {
                if (((Float) o).isInfinite() || ((Float) o).isNaN()) {
                    throw new JsonException("JSON does not allow non-finite numbers.");
                }
            }
        }
    }

    static void indent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i += 1) {
            writer.write(' ');
        }
    }

    /**
     * Produce a string from a Number.
     *
     * @param number A Number
     * @return A String.
     */
    static String numberToString(Number number) {
        if (number == null) {
            throw new IllegalArgumentException("Null pointer");
        }
        try {
            JsonUtils.testValidity(number);
        } catch (JsonException e) {
            throw new IllegalArgumentException(e.getMessage());
        }

        // Shave off trailing zeros and decimal point, if possible.

        String string = number.toString();
        if (string.indexOf('.') > 0 && string.indexOf('e') < 0 && string.indexOf('E') < 0) {
            while (string.endsWith("0")) {
                string = string.substring(0, string.length() - 1);
            }
            if (string.endsWith(".")) {
                string = string.substring(0, string.length() - 1);
            }
        }
        return string;
    }

    /**
     * Produce a string in double quotes with backslash sequences in all the
     * right places. A backslash will be inserted within </, producing <\/,
     * allowing JSON text to be delivered in HTML. In JSON text, a string cannot
     * contain a control character or an unescaped quote or backslash.
     *
     * @param string A String
     * @return A String correctly formatted for insertion in a JSON text.
     */
    static String quote(String string) {
        StringWriter sw = new StringWriter();
        try {
            return quote(string, sw).toString();
        } catch (IOException ignored) {
            // will never happen - we are writing to a string writer
            return "";
        }
    }

    static Writer quote(String string, Writer w) throws IOException {
        if (string == null || string.length() == 0) {
            w.write("\"\"");
            return w;
        }

        char b;
        char c = 0;
        String hhhh;
        int i;
        int len = string.length();

        w.write('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    w.write('\\');
                    w.write(c);
                    break;
                case '/':
                    if (b == '<') {
                        w.write('\\');
                    }
                    w.write(c);
                    break;
                case '\b':
                    w.write("\\b");
                    break;
                case '\t':
                    w.write("\\t");
                    break;
                case '\n':
                    w.write("\\n");
                    break;
                case '\f':
                    w.write("\\f");
                    break;
                case '\r':
                    w.write("\\r");
                    break;
                default:
                    if (c < ' ' || c >= '\u0080' && c < '\u00a0' || c >= '\u2000' && c < '\u2100') {
                        w.write("\\u");
                        hhhh = Integer.toHexString(c);
                        w.write("0000", 0, 4 - hhhh.length());
                        w.write(hhhh);
                    } else {
                        w.write(c);
                    }
            }
        }
        w.write('"');
        return w;
    }

    public static String[] splitJPath(String jPath) {
        // trim leading slashes
        int start;
        for (start = 0; start < jPath.length(); start++) {
            char character = jPath.charAt(start);
            if (character != '/') {
                break;
            }
        }
        // split into head and tail
        int end;
        for (end = start; end < jPath.length(); end++) {
            char character = jPath.charAt(end);
            if (end > 0 && (character == '/' || character == '[')) {
                break;
            }
        }
        String head = jPath.substring(start, end);
        String tail = jPath.substring(end);
        return new String[]{head, tail};
    }

    /**
     * Try to convert a string into a number, boolean, or null. If the string
     * can't be converted, return the string.
     *
     * @param string A String.
     * @return A simple JSON value.
     */
    static Object stringToValue(String string) {
        if (string.equals("")) {
            return string;
        }
        if (string.equalsIgnoreCase("true")) {
            return Boolean.TRUE;
        }
        if (string.equalsIgnoreCase("false")) {
            return Boolean.FALSE;
        }
        if (string.equalsIgnoreCase("null")) {
            return null;
        }

        /*
         * If it might be a number, try converting it. If a number cannot be
         * produced, then the value will just be a string.
         */

        char b = string.charAt(0);
        if (b >= '0' && b <= '9' || b == '-') {
            try {
                if (string.indexOf('.') > -1 || string.indexOf('e') > -1 || string.indexOf('E') > -1) {
                    Double d = Double.valueOf(string);
                    if (!d.isInfinite() && !d.isNaN()) {
                        return d;
                    }
                } else {
                    long myLong = Long.parseLong(string);
                    if (string.equals(Long.toString(myLong))) {
                        if (myLong == (int) myLong) {
                            return (int) myLong;
                        } else {
                            return myLong;
                        }
                    }
                }
            } catch (Exception ignore) {
            }
        }
        return string;
    }

    static Writer writeValue(Writer writer, Object value, int indentFactor, int indent) throws IOException {
        if (value == null) {
            writer.write("null");
        } else if (value instanceof JsonObject) {
            ((JsonObject) value).write(writer, indentFactor, indent);
        } else if (value instanceof JsonArray) {
            ((JsonArray) value).write(writer, indentFactor, indent);
        } else if (value instanceof Map) {
            new JsonObject((Map<?, ?>) value).write(writer, indentFactor, indent);
        } else if (value instanceof Collection) {
            new JsonArray((Collection<?>) value).write(writer, indentFactor, indent);
        } else if (value.getClass().isArray()) {
            try {
                new JsonArray(value).write(writer, indentFactor, indent);
            } catch (JsonException e) {
                // JsonArray constructor with Array will never throw
                throw new IllegalStateException(e);
            }
        } else if (value instanceof Number) {
            writer.write(numberToString((Number) value));
        } else if (value instanceof Boolean) {
            writer.write(value.toString());
        } else {
            quote(value.toString(), writer);
        }
        return writer;
    }

    public static List<Integer> toIntegerList(String jsonString) {
        if (jsonString == null) {
            return new ArrayList<>();
        }
        try {
            return toIntegerList(new JsonArray(jsonString));
        } catch (Exception e) {
        }
        return new ArrayList<>();
    }

    public static Set<String> toStringSet(JsonArray jsonArray) {
        if (jsonArray == null) {
            return new HashSet<>();
        }

        Set<String> set = new LinkedHashSet<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            set.add(jsonArray.tryGetString(i));
        }

        return set;
    }

    public static List<String> toStringList(JsonArray jsonArray) {
        if (jsonArray == null) {
            return new ArrayList<>();
        }

        List<String> list = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(jsonArray.tryGetString(i));
        }

        return list;
    }

    public static List<Integer> toIntegerList(JsonArray jsonArray) {
        if (jsonArray == null) {
            return new ArrayList<>();
        }

        List<Integer> list = new ArrayList<>();

        for (int i = 0; i < jsonArray.size(); i++) {
            list.add(jsonArray.tryGetInt(i));
        }

        return list;
    }

    public static <T extends Jsonable> JsonArray toJsonObjectArray(List<T> objects) {
        JsonArray jsArray = new JsonArray();
        if (objects == null) {
            return jsArray;
        }
        for (Jsonable obj : objects) {
            jsArray.add(obj.asJson());
        }

        return jsArray;
    }

    public static JsonObject filterFields(JsonObject srcObject, Set<String> fields) {
        JsonObject filteredJson = new JsonObject();
        if (srcObject == null) {
            return filteredJson;
        }
        for (String srcKey : srcObject.keySet()) {
            if (fields.contains(srcKey)) {
                filteredJson.put(srcKey, srcObject.get(srcKey));
            }
        }

        return filteredJson;
    }

    public static JsonArray snakeCaseKeys(JsonArray jso) {
        for (int i = 0; i < jso.size(); i++) {
            Object o = jso.get(i);
            if (o instanceof JsonObject) {
                snakeCaseKeys((JsonObject) o);
            }
        }

        return jso;
    }

    /**
     * Rewrite key names in a json object.
     */
    public static JsonObject snakeCaseKeys(JsonObject jso) {
        Pattern matchPattern = PatternHelper.compileOrGet("(?<=[a-z])([A-Z])");
        Set<String> keys = new HashSet<>(jso.keySet());
        for (String key : keys) {
            String newKey = matchPattern.matcher(key).replaceAll("_$1").toLowerCase();
            if (newKey.equals(key)) {
                continue;
            }
            jso.put(newKey, jso.get(key));
            jso.remove(key);
        }
        keys = jso.keySet();
        for (String key : keys) {
            JsonObject child = jso.tryGetJsonObject(key);
            if (child != null) {
                snakeCaseKeys(child);
            }
            JsonArray childArray = jso.tryGetJsonArray(key);
            if (childArray != null) {
                for (Object o : childArray) {
                    if (o instanceof JsonObject) {
                        snakeCaseKeys((JsonObject) o);
                    }
                }
            }
        }

        return jso;
    }

    /**
     * Rewrite key names in a json object.
     */
    public static void replace(JsonObject json, String fieldName, String newFieldName) {
        if (json.containsKey(fieldName)) {
            json.put(newFieldName, json.get(fieldName));
            json.remove(fieldName);
        }
        for (String key : json.keySet()) {
            Object value = json.get(key);
            if (value instanceof JsonObject) {
                replace((JsonObject) value, fieldName, newFieldName);
            } else if (value instanceof JsonArray) {
                for (Object o : (JsonArray) value) {
                    if (o instanceof JsonObject) {
                        replace((JsonObject) o, fieldName, newFieldName);
                    }
                }
            }
        }
    }

    public static void remove(JsonArray jsonArray, String fieldToRemove) {
        for (Object o : jsonArray) {
            if (o instanceof JsonObject) {
                remove((JsonObject) o, fieldToRemove);
            }
        }
    }

    public static void remove(JsonObject jso, String fieldToRemove) {
        jso.remove(fieldToRemove);
        for (String key : jso.keySet()) {
            Object value = jso.get(key);
            if (value instanceof JsonObject) {
                remove((JsonObject) value, fieldToRemove);
            } else if (value instanceof JsonArray) {
                remove((JsonArray) value, fieldToRemove);
            }
        }
    }

    public static void valueReplace(JsonArray jsonArray, String fieldName, Pattern pattern, String replacement) {
        Set<String> toRemove = new HashSet<>();
        Set<String> toAdd = new HashSet<>();
        for (Object o : jsonArray) {
            if (o instanceof JsonObject) {
                valueReplace((JsonObject) o, fieldName, pattern, replacement);
            } else if (o instanceof String) {
                toRemove.add((String) o);
                toAdd.add(pattern.matcher((String) o).replaceAll(replacement));
            }
        }
        jsonArray.removeAll(toRemove);
        jsonArray.addAll(toAdd);
    }

    public static void valueReplace(JsonObject jso, String fieldName, Pattern pattern, String replacement) {
        Object value = jso.get(fieldName);
        if (value instanceof String) {
            jso.put(fieldName, pattern.matcher((String) value).replaceAll(replacement));
        }
        for (String key : jso.keySet()) {
            value = jso.get(key);
            if (value instanceof JsonObject) {
                valueReplace((JsonObject) value, fieldName, pattern, replacement);
            } else if (value instanceof JsonArray) {
                valueReplace((JsonArray) value, fieldName, pattern, replacement);
            }
        }
    }
}
