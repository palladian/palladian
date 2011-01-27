package tud.iir.persistence;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * A simple {@link RowConverter} implementation, converting {@link ResultSet} rows to {@link Map}s. Each column from the
 * ResultSet is transformed to a Map entry.
 * 
 * @author Philipp Katz
 */
public class SimpleRowConverter implements RowConverter<Map<String, Object>> {

    @Override
    public Map<String, Object> convert(ResultSet resultSet) throws SQLException {
        Map<String, Object> result = new HashMap<String, Object>();
        ResultSetMetaData metaData = resultSet.getMetaData();
        int columnCount = metaData.getColumnCount();
        for (int i = 0; i < columnCount; i++) {
            String columnName = metaData.getColumnName(i + 1);
            Object object = resultSet.getObject(i + 1);
            result.put(columnName, object);
        }
        return result;
    }

}
