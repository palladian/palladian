package ws.palladian.persistence.json;

import ws.palladian.helper.functional.Factory;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Build a json object.
 * </p>
 *
 * @author David Urbansky
 */
public final class JsonObjectBuilder implements Factory<JsonObject> {
    private final JsonObject json;

    /**
     * <p>
     * Create a new {@link JsonObjectBuilder} initialized with a copy of the supplied {@link JsonObject}.
     * </p>
     *
     * @param jsonObject The jsonObject with entries to add to this builder.
     * @return The builder. Use {@link #put(String, Object)} to add further entries.
     */
    public static JsonObjectBuilder createWith(JsonObject jsonObject) {
        return new JsonObjectBuilder(jsonObject);
    }

    /**
     * <p>
     * Create a new {@link JsonObjectBuilder } initialized with a {@link HashMap}.
     * </p>
     *
     * @return The builder. Use {@link #put(String, Object)} to add further entries.
     */
    public static JsonObjectBuilder createEmpty() {
        return new JsonObjectBuilder();
    }

    /**
     * <p>
     * Create a new {@link JsonObjectBuilder} initialized with a {@link JsonObject and add the supplied key-value pair.
     * </p>
     *
     * @param key   The key to add.
     * @param value The value to add.
     * @return The builder. Use {@link #put(String, Object)} to add further entries.
     */
    public static JsonObjectBuilder createPut(String key, Object value) {
        return new JsonObjectBuilder().put(key, value);
    }

    private JsonObjectBuilder() {
        this.json = new JsonObject();
    }

    private JsonObjectBuilder(Map map) {
        this.json = new JsonObject(map);
    }

    /**
     * <p>
     * Put a new key/value pair to the map, return {@link JsonObjectBuilder} to allow method chaining.
     * </p>
     *
     * @param key   Key.
     * @param value Value.
     * @return The builder. Use {@link #put(String, Object)} to add further entries.
     */
    public JsonObjectBuilder put(String key, Object value) {
        json.put(key, value);
        return this;
    }

    @Override
    public JsonObject create() {
        return json;
    }

}
