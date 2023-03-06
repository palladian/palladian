package ws.palladian.persistence.json;

/**
 * Objects that can be serialized to json implement this interface.
 *
 * @author David Urbansky
 */
public interface Jsonable {
    JsonObject asJson();
}
