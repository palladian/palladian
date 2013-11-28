package ws.palladian.persistence;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Convert all columns of a given result set to strings and return them in a key-value map.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class MapRowConverter implements RowConverter<Map<String, Object>> {

    public final static RowConverter<Map<String, Object>> MAP = new MapRowConverter();

    private MapRowConverter() {
        // the one and only.
    }

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

}
