package ws.palladian.persistence;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Various predefined {@link RowConverter}s.
 * 
 * @author pk
 */
public final class RowConverters {

    /** {@link RowConverter} for {@link Boolean} types. */
    public final static RowConverter<Boolean> BOOLEAN = new RowConverter<Boolean>() {
        @Override
        public Boolean convert(ResultSet resultSet) throws SQLException {
            return resultSet.getBoolean(1);
        }
    };

    /** {@link RowConverter} for {@link Integer} types. */
    public final static RowConverter<Integer> INTEGER = new RowConverter<Integer>() {
        @Override
        public Integer convert(ResultSet resultSet) throws SQLException {
            return resultSet.getInt(1);
        }
    };

    /** {@link RowConverter} for {@link Long} types. */
    public final static RowConverter<Long> LONG = new RowConverter<Long>() {
        @Override
        public Long convert(ResultSet resultSet) throws SQLException {
            return resultSet.getLong(1);
        }
    };

    /** {@link RowConverter} for {@link Double} types. */
    public final static RowConverter<Double> DOUBLE = new RowConverter<Double>() {
        @Override
        public Double convert(ResultSet resultSet) throws SQLException {
            return resultSet.getDouble(1);
        }
    };

    /** {@link RowConverter} for {@link String} types. */
    public final static RowConverter<String> STRING = new RowConverter<String>() {
        @Override
        public String convert(ResultSet resultSet) throws SQLException {
            return resultSet.getString(1);
        }
    };
    
    /** {@link RowConverter} for {@link Object} types. */
    public static final RowConverter<Object> OBJECT = new RowConverter<Object>() {
        @Override
        public Object convert(ResultSet resultSet) throws SQLException {
            return resultSet.getObject(1);
        }
    };

    /** {@link RowConverter} for converting all columns to a map. */
    public final static RowConverter<Map<String, Object>> MAP = new RowConverter<Map<String, Object>>() {
        @Override
        public Map<String, Object> convert(ResultSet resultSet) throws SQLException {
            Map<String, Object> map = new HashMap<String, Object>();
            ResultSetMetaData metaData = resultSet.getMetaData();
            int numColumns = metaData.getColumnCount();
            for (int i = 1; i <= numColumns; i++) {
                String label = metaData.getColumnLabel(i);
                Object value = resultSet.getObject(i);
                map.put(label, value);
            }
            return map;
        }
    };

    private RowConverters() {
        // prevent instances.
    }

}
