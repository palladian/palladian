package ws.palladian.retrieval;

/**
 * HTTP methods, aka "verbs".
 * 
 * @author Philipp Katz
 */
public enum HttpMethod {
    GET, POST, HEAD, PUT, DELETE, PATCH;
    /**
     * @return An array with string values of all HTTP methods.
     */
    public static String[] stringValues() {
        HttpMethod[] values = values();
        String[] stringValues = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            stringValues[i] = values[i].name();
        }
        return stringValues;
    }
}
