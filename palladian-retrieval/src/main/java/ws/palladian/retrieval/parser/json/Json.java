package ws.palladian.retrieval.parser.json;

import java.io.Writer;

/**
 * <p>
 * Common interface for JSON.
 * </p>
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
 * @author Philipp Katz
 */
public interface Json {

    /**
     * <p>
     * Write the contents of the {@link Json} as JSON text to a writer. For compactness, no whitespace is added.
     * <b>Warning:</b> This method assumes that the data structure is acyclical.
     * </p>
     * 
     * @return The writer.
     * @throws JsonException
     */
    Writer write(Writer writer);

    /**
     * <p>
     * Make a prettyprinted JSON text of this {@link Json}. <b>Warning:</b> This method assumes that the data structure
     * is acyclical.
     * </p>
     * 
     * @param indentFactor The number of spaces to add to each level of indentation.
     * @return a printable, displayable, transmittable representation of the object.
     * @throws JsonException
     */
    String toString(int indentFactor);

    /**
     * <p>
     * Perform a "JPath" query and return an {@link Object}. Hint: Use the typed <code>query</code> methods (e.g.
     * {@link #queryBoolean(String)}) to specify the desired return type.
     * </p>
     * 
     * @param jPath The JPath.
     * @return The retrieved object, or <code>null</code> in case no object could be retrieved with the given path.
     */
    Object query(String jPath);

    /**
     * <p>
     * Perform a "JPath" query and return a {@link Boolean}.
     * </p>
     * 
     * @param jPath The JPath.
     * @return The retrieved object, or <code>null</code> in case no object could be retrieved with the given path, or
     *         the object at the given path could not be parsed as boolean.
     */
    Boolean queryBoolean(String jPath);

    /**
     * <p>
     * Perform a "JPath" query and return a {@link Double}.
     * </p>
     * 
     * @param jPath The JPath.
     * @return The retrieved object, or <code>null</code> in case no object could be retrieved with the given path, or
     *         the object at the given path could not be parsed as double.
     */
    Double queryDouble(String jPath);

    /**
     * <p>
     * Perform a "JPath" query and return an {@link Integer}.
     * </p>
     * 
     * @param jPath The JPath.
     * @return The retrieved object, or <code>null</code> in case no object could be retrieved with the given path, or
     *         the object at the given path could not be parsed as integer.
     */
    Integer queryInt(String jPath);

    /**
     * <p>
     * Perform a "JPath" query for a {@link JsonArray}.
     * </p>
     * 
     * @param jPath The JPath.
     * @return The retrieved JsonArray, or <code>null</code> in case no JsonArray object could be retrieved at the given
     *         path.
     */
    JsonArray queryJsonArray(String jPath);

    /**
     * <p>
     * Perform a "JPath" query for a {@link JsonObject}.
     * </p>
     * 
     * @param jPath The JPath.
     * @return The retrieved JsonObject, or <code>null</code> in case no JsonArray object could be retrieved at the
     *         given path.
     */
    JsonObject queryJsonObject(String jPath);

    /**
     * <p>
     * Perform a "JPath" query and return a {@link Long}.
     * </p>
     * 
     * @param jPath The JPath.
     * @return The retrieved object, or <code>null</code> in case no object could be retrieved with the given path, or
     *         the object at the given path could not be parsed as long.
     */
    Long queryLong(String jPath);

    /**
     * <p>
     * Perform a "JPath" query and return an {@link String}.
     * </p>
     * 
     * @param jPath The JPath.
     * @return The retrieved object, or <code>null</code> in case no object could be retrieved with the given path, or
     *         the object at the given path is no string.
     */
    String queryString(String jPath);

    /**
     * @return The number of entries in the {@link Json} object.
     */
    int size();

}
