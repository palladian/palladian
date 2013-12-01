package ws.palladian.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Convert all columns of a given result set to strings and return them in a key-value map.
 * </p>
 * 
 * @author David Urbansky
 * @deprecated Use {@link MapRowConverter}.
 */
@Deprecated
public final class AllColumnsRowConverter {

    private AllColumnsRowConverter() {
        // prevent instances.
    }

    /**
     * <p>
     * A {@link RowConverter} for {@link Map<String,Object>} types.
     * </p>
     */
    public final static RowConverter<Map<String, Object>> MAP = new RowConverter<Map<String, Object>>() {
        @Override
        public Map<String, Object> convert(ResultSet resultSet) throws SQLException {

            Map<String, Object> map = new HashMap<String, Object>();

            int numColumns = resultSet.getMetaData().getColumnCount();
            for (int i = 1; i <= numColumns; i++) {
                String columnName = resultSet.getMetaData().getColumnLabel(i);
                int columnType = resultSet.getMetaData().getColumnType(i);

                if (columnType == Types.INTEGER) {
                    map.put(columnName, resultSet.getInt(i));
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
        }
    };

}
