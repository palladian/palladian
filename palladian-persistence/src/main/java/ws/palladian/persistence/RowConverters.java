package ws.palladian.persistence;

import java.sql.ResultSetMetaData;
import java.sql.Types;
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

    /** {@link RowConverter} for converting all columns to a map. */
    public final static RowConverter<Map<String, Object>> MAP = resultSet -> {
        Map<String, Object> map = new HashMap<>();

        ResultSetMetaData metaData = resultSet.getMetaData();
        int numColumns = metaData.getColumnCount();
        for (int i = 1; i <= numColumns; i++) {
            String columnName = metaData.getColumnLabel(i);
            int columnType = metaData.getColumnType(i);

            if (resultSet.getObject(i) == null) {
                map.put(columnName, null);
            } else if (columnType == Types.INTEGER) {
                try {
                    map.put(columnName, resultSet.getInt(i));
                } catch (Exception e) { // unsigned int might throw an exception, try again parsing as long
                    map.put(columnName, resultSet.getLong(i));
                }
            } else if (columnType == Types.TINYINT) {
                map.put(columnName, resultSet.getInt(i));
            } else if (columnType == Types.SMALLINT) {
                map.put(columnName, resultSet.getInt(i));
            } else if (columnType == Types.NUMERIC) {
                map.put(columnName, resultSet.getInt(i));
            } else if (columnType == Types.BIGINT) {
                map.put(columnName, resultSet.getLong(i));
            } else if (columnType == Types.FLOAT) {
                map.put(columnName, resultSet.getDouble(i));
            } else if (columnType == Types.DOUBLE) {
                map.put(columnName, resultSet.getDouble(i));
            } else if (columnType == Types.BOOLEAN) {
                map.put(columnName, resultSet.getBoolean(i));
            } else if (columnType == Types.TIMESTAMP) {
                map.put(columnName, resultSet.getTimestamp(i));
            } else if (columnType == Types.DATE) {
                map.put(columnName, resultSet.getDate(i));
            } else {
                map.put(columnName, resultSet.getString(i));
            }
        }

        return map;
    };

    private RowConverters() {
        // prevent instances.
    }
}
