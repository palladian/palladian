package ws.palladian.persistence;

import java.util.Map;

/**
 * <p>
 * Convert all columns of a given result set to strings and return them in a key-value map.
 * </p>
 * 
 * FIXME unlike the AllColumnRowConverter, this class seems to incorrectly interpret mysql data types, e.g. Integer
 * becomes Long.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
@Deprecated
public final class MapRowConverter {

    /** @deprecated Use {@link RowConverters#MAP} instead */
    @Deprecated
    public final static RowConverter<Map<String, Object>> MAP = RowConverters.MAP;

    private MapRowConverter() {
        // the one and only.
    }

}
