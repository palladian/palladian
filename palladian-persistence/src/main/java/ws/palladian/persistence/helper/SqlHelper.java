package ws.palladian.persistence.helper;

import java.util.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

/**
 * @author Sandro Reichert
 * @author Philipp Katz
 */
public class SqlHelper {

    /**
     * Helper to get an Integer value from a result set that preserves <code>null</code> values.
     * 
     * <p>
     * SQL only provides a getInt method that returns an int value. In case the original value in the database was
     * <code>null</code>, it returns 0.
     * </p>
     * 
     * @param resultSet The resultSet to get the Integer value from.
     * @param columnLabel The label of the column to get the value from.
     * @return The Integer of the column or <code>null</code> if <code>null</code> is read from database.
     * @throws SQLException
     */
    public static Integer getInteger(ResultSet resultSet, String columnLabel) throws SQLException {
        Integer value = null;
        synchronized (resultSet) {
            value = resultSet.getInt(columnLabel);
            value = resultSet.wasNull() ? null : value;
        }
        return value;
    }

    /**
     * Helper to get an Boolean value from a result set that preserves <code>null</code> values.
     * 
     * <p>
     * SQL only provides a getBoolean method that returns an int value. In case the original value in the database was
     * <code>null</code>, it returns 0.
     * </p>
     * 
     * @param resultSet The resultSet to get the Boolean value from.
     * @param columnLabel The label of the column to get the value from.
     * @return The Boolean value of the column or <code>null</code> if <code>null</code> is read from database.
     * @throws SQLException
     */
    public static Boolean getBoolean(ResultSet resultSet, String columnLabel) throws SQLException {
        Boolean value = null;
        synchronized (resultSet) {
            value = resultSet.getBoolean(columnLabel);
            value = resultSet.wasNull() ? null : value;
        }
        return value;
    }

    /**
     * <p>
     * Convert a {@link Date} to a SQL {@link Timestamp}.
     * </p>
     * 
     * @param date The date to convert.
     * @return The {@link Timestamp}, or <code>null</code> if date was <code>null</code>.
     */
    public static Timestamp getTimestamp(Date date) {
        if (date == null) {
            return null;
        }
        return new Timestamp(date.getTime());
    }
}
