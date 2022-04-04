package ws.palladian.persistence;

import java.sql.ResultSetMetaData;
import java.util.HashMap;
import java.util.Map;

/**
 * Various predefined {@link RowConverter}s.
 * 
 * @author Philipp Katz
 */
public final class RowConverters {

    /** {@link RowConverter} for {@link Boolean} types. */
    public final static RowConverter<Boolean> BOOLEAN = resultSet -> resultSet.getBoolean(1);

    /** {@link RowConverter} for {@link Integer} types. */
    public final static RowConverter<Integer> INTEGER = resultSet -> resultSet.getInt(1);

    /** {@link RowConverter} for {@link Long} types. */
    public final static RowConverter<Long> LONG = resultSet -> resultSet.getLong(1);

    /** {@link RowConverter} for {@link Double} types. */
    public final static RowConverter<Double> DOUBLE = resultSet -> resultSet.getDouble(1);

    /** {@link RowConverter} for {@link String} types. */
    public final static RowConverter<String> STRING = resultSet -> resultSet.getString(1);
    
    /** {@link RowConverter} for {@link Object} types. */
    public static final RowConverter<Object> OBJECT = resultSet -> resultSet.getObject(1);

    /** {@link RowConverter} for converting all columns to a map. FIXME this does not replace {@link AllColumnsRowConverter.MAP} properly as it does not handle datatypes correctly */
    public final static RowConverter<Map<String, Object>> MAP = resultSet -> {
	    Map<String, Object> map = new HashMap<>();
	    ResultSetMetaData metaData = resultSet.getMetaData();
	    int numColumns = metaData.getColumnCount();
	    for (int i = 1; i <= numColumns; i++) {
	        String label = metaData.getColumnLabel(i);
	        Object value = resultSet.getObject(i);
	        map.put(label, value);
	    }
	    return map;
	};

    private RowConverters() {
        // prevent instances.
    }

}
