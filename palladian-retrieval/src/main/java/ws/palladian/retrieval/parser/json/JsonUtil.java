package ws.palladian.retrieval.parser.json;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

class JsonUtil {

    public static boolean parseBoolean(Object object) throws JsonException {
        try {
            if (object.equals(Boolean.FALSE) || object instanceof String && ((String)object).equalsIgnoreCase("false")) {
                return false;
            } else if (object.equals(Boolean.TRUE) || object instanceof String
                    && ((String)object).equalsIgnoreCase("true")) {
                return true;
            }
        } catch (Exception e) {
        }
        throw new JsonException("Could not parse \"" + object + "\" to boolean.");
    }

    public static double parseDouble(Object object) throws JsonException {
        try {
            return object instanceof Number ? ((Number)object).doubleValue() : Double.parseDouble((String)object);
        } catch (Exception e) {
            throw new JsonException("Could not parse \"" + object + "\" to double.");
        }
    }

    public static int parseInt(Object object) throws JsonException {
        try {
            return object instanceof Number ? ((Number)object).intValue() : Integer.parseInt((String)object);
        } catch (Exception e) {
            throw new JsonException("Could not parse \"" + object + "\" to int.");
        }
    }

    public static JsonArray parseJSONArray(Object object) throws JsonException {
        try {
            return object instanceof JsonArray ? (JsonArray)object : null;
        } catch (Exception e) {
            throw new JsonException("Could not parse \"" + object + "\" to JSON array.");
        }
    }

    public static JsonObject parseJSONObject(Object object) throws JsonException {
        try {
            return object instanceof JsonObject ? (JsonObject)object : null;
        } catch (Exception e) {
            throw new JsonException("Could not parse \"" + object + "\" to JSON object.");
        }
    }

    public static Long parseLong(Object object) throws JsonException {
        try {
            return object instanceof Number ? ((Number)object).longValue() : Long.parseLong((String)object);
        } catch (Exception e) {
            throw new JsonException("Could not parse \"" + object + "\" to long.");
        }
    }

    public static String parseString(Object object) throws JsonException {
        try {
            return object instanceof String ? (String)object : null;
        } catch (Exception e) {
            throw new JsonException("Could not parse \"" + object + "\" to string.");
        }
    }

    /**
     * Throw an exception if the object is a NaN or infinite number.
     * 
     * @param o
     *            The object to test.
     * @throws JsonException
     *             If o is a non-finite number.
     */
    public static void testValidity(Object o) throws JsonException {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double)o).isInfinite() || ((Double)o).isNaN()) {
                    throw new JsonException("JSON does not allow non-finite numbers.");
                }
            } else if (o instanceof Float) {
                if (((Float)o).isInfinite() || ((Float)o).isNaN()) {
                    throw new JsonException("JSON does not allow non-finite numbers.");
                }
            }
        }
    }

    /**
     * Try to convert a string into a number, boolean, or null. If the string
     * can't be converted, return the string.
     * 
     * @param string
     *            A String.
     * @return A simple JSON value.
     */
    static Object stringToValue(String string) {
        Double d;
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
                    d = Double.valueOf(string);
                    if (!d.isInfinite() && !d.isNaN()) {
                        return d;
                    }
                } else {
                    Long myLong = new Long(string);
                    if (string.equals(myLong.toString())) {
                        if (myLong.longValue() == myLong.intValue()) {
                            return new Integer(myLong.intValue());
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

    static final Writer writeValue(Writer writer, Object value, int indentFactor, int indent) throws IOException {
        if (value == null || value.equals(null)) {
            writer.write("null");
        } else if (value instanceof JsonObject) {
            ((JsonObject)value).write(writer, indentFactor, indent);
        } else if (value instanceof JsonArray) {
            ((JsonArray)value).write(writer, indentFactor, indent);
        } else if (value instanceof Map) {
            new JsonObject((Map<?, ?>)value).write(writer, indentFactor, indent);
        } else if (value instanceof Collection) {
            new JsonArray((Collection<?>)value).write(writer, indentFactor, indent);
        } else if (value.getClass().isArray()) {
            new JsonArray(value).write(writer, indentFactor, indent);
        } else if (value instanceof Number) {
            writer.write(numberToString((Number)value));
        } else if (value instanceof Boolean) {
            writer.write(value.toString());
        } else {
            quote(value.toString(), writer);
        }
        return writer;
    }

    static final void indent(Writer writer, int indent) throws IOException {
        for (int i = 0; i < indent; i += 1) {
            writer.write(' ');
        }
    }

    /**
     * Produce a string from a Number.
     * 
     * @param number
     *            A Number
     * @return A String.
     * @throws JsonException
     *             If n is a non-finite number.
     */
    static String numberToString(Number number) {
        if (number == null) {
            throw new IllegalArgumentException("Null pointer");
        }
        try {
            JsonUtil.testValidity(number);
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
     * @param string
     *            A String
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
        return new String[] {head, tail};
    }

    private JsonUtil() {
        // util.
    }

}
