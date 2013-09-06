package ws.palladian.persistence.helper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Date;

/**
 * <p>
 * Helper to get an Integer/Long/Double/Boolean/String values from a result set that preserves <code>null</code> values.
 * </p>
 * 
 * @author Sandro Reichert
 * @author David Urbansky
 * @author Philipp Katz
 */
public class SqlHelper {

    /**
     * <p>
     * Helper to get an Integer value from a result set that preserves <code>null</code> values.
     * </p>
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
     * <p>
     * Helper to get a Long value from a result set that preserves <code>null</code> values.
     * </p>
     * 
     * <p>
     * SQL only provides a getLong method that returns an Long value. In case the original value in the database was
     * <code>null</code>, it returns 0.
     * </p>
     * 
     * @param resultSet The resultSet to get the Long value from.
     * @param columnLabel The label of the column to get the value from.
     * @return The Long of the column or <code>null</code> if <code>null</code> is read from database.
     * @throws SQLException
     */
    public static Long getLong(ResultSet resultSet, String columnLabel) throws SQLException {
        Long value = null;
        synchronized (resultSet) {
            value = resultSet.getLong(columnLabel);
            value = resultSet.wasNull() ? null : value;
        }
        return value;
    }

    /**
     * <p>
     * Helper to get a Double value from a result set that preserves <code>null</code> values.
     * </p>
     * 
     * <p>
     * SQL only provides a getDouble method that returns an Double value. In case the original value in the database was
     * <code>null</code>, it returns 0.
     * </p>
     * 
     * @param resultSet The resultSet to get the Double value from.
     * @param columnLabel The label of the column to get the value from.
     * @return The Long of the column or <code>null</code> if <code>null</code> is read from database.
     * @throws SQLException
     */
    public static Double getDouble(ResultSet resultSet, String columnLabel) throws SQLException {
        Double value = null;
        synchronized (resultSet) {
            value = resultSet.getDouble(columnLabel);
            value = resultSet.wasNull() ? null : value;
        }
        return value;
    }

    /**
     * <p>
     * Helper to get an Boolean value from a result set that preserves <code>null</code> values.
     * </p>
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
     * Helper to get a String value from a result set that preserves <code>null</code> values.
     * </p>
     * 
     * <p>
     * SQL only provides a getString method that returns an String value. In case the original value in the database was
     * <code>null</code>, it returns 0.
     * </p>
     * 
     * @param resultSet The resultSet to get the String value from.
     * @param columnLabel The label of the column to get the value from.
     * @return The String of the column or <code>null</code> if <code>null</code> is read from database.
     * @throws SQLException
     */
    public static String getString(ResultSet resultSet, String columnLabel) throws SQLException {
        String value = null;
        synchronized (resultSet) {
            value = resultSet.getString(columnLabel);
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
