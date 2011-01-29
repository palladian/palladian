package tud.iir.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

/**
 * The DatabaseManager writes and reads data to the database.
 * 
 * @author David Urbansky
 * @author Christopher Friedrich
 * @author Philipp Katz
 * @author Martin Werner
 */
public class DatabaseManager {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatabaseManager.class);

    /**
     * Get a {@link Connection} from the {@link ConnectionManager}.
     * 
     * @return
     * @throws SQLException
     */
    protected final Connection getConnection() throws SQLException {
        return ConnectionManager.getInstance().getConnection();
    }

    /**
     * Run a query operation on the database.
     * 
     * @param <T>
     * @param callback
     * @param converter
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Number of processed results.
     */
    public final <T> int runQuery(ResultCallback<T> callback, RowConverter<T> converter, String sql, Object... args) {

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int counter = 0;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(sql);

            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }

            rs = ps.executeQuery();

            while (rs.next() && callback.isLooping()) {
                T item = converter.convert(rs);
                callback.processResult(item, ++counter);
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(connection, ps, rs);
        }

        return counter;

    }

    /**
     * 
     * @param callback
     * @param sql
     * @param args
     * @return
     */
    public final int runQuery(ResultCallback<Map<String, Object>> callback, String sql, Object... args) {
        return runQuery(callback, new SimpleRowConverter(), sql, args);
    }

    /**
     * 
     * @param <T>
     * @param converter
     * @param sql
     * @param args
     * @return
     */
    public final <T> List<T> runQuery(RowConverter<T> converter, String sql, Object... args) {

        final List<T> result = new ArrayList<T>();

        ResultCallback<T> callback = new ResultCallback<T>() {

            @Override
            public void processResult(T object, int number) {
                result.add(object);
            }

        };

        runQuery(callback, converter, sql, args);

        return result;
    }

    /**
     * Allows to iterate over available items without buffering the content of the whole result in memory first, using a
     * standard Iterator interface. The underlying Iterator implementation does not allow modifications, so a call to
     * {@link Iterator#remove()} will cause an {@link UnsupportedOperationException}. The ResultSet which is used by the
     * implementation is closed, after the last element has been retrieved, if you break the iteration loop, you must
     * manually call {@link ResultIterator#close()}}.
     * 
     * @param <T>
     * @param converter
     * @param sql
     * @param args
     * @return
     */
    public final <T> ResultIterator<T> runQueryWithIterator(RowConverter<T> converter, String sql, Object... args) {

        ResultIterator<T> result = null;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(sql);

            // do not buffer the whole ResultSet in memory, but use streaming to save memory
            // http://webmoli.com/2009/02/01/jdbc-performance-tuning-with-optimal-fetch-size/
            // TODO make this a global option?
            ps.setFetchSize(Integer.MIN_VALUE);

            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }

            resultSet = ps.executeQuery();
            result = new ResultIterator<T>(connection, ps, resultSet, converter);

        } catch (SQLException e) {
            LOGGER.error(e);
            close(connection, ps, resultSet);
        }

        return result;

    }

    /**
     * 
     * @param <T>
     * @param converter
     * @param sql
     * @param args
     * @return
     */
    public final <T> ResultIterator<T> runQueryWithIterator(RowConverter<T> converter, String sql, List<Object> args) {
        return runQueryWithIterator(converter, sql, args.toArray());
    }

    /**
     * 
     * @param <T>
     * @param converter
     * @param sql
     * @param args
     * @return
     */
    @SuppressWarnings("unchecked")
    public final <T> T runSingleQuery(RowConverter<T> converter, String sql, Object... args) {

        final Object[] result = new Object[1];

        ResultCallback<T> callback = new ResultCallback<T>() {

            @Override
            public void processResult(T object, int number) {
                result[0] = object;
                breakLoop();
            }
        };

        runQuery(callback, converter, sql, args);

        return (T) result[0];

    }

    public long runCountQuery(String countQuery) {

        final Integer[] count = new Integer[1];

        ResultCallback<Map<String, Object>> callback = new ResultCallback<Map<String, Object>>() {

            @Override
            public void processResult(Map<String, Object> object, int number) {
                count[0] = (Integer) object.get("count");
            }
        };

        runQuery(callback, countQuery);

        if (count[0] == null) {
            count[0] = -1;
        }

        return count[0];
    }

    /**
     * 
     * @param sql
     * @param args
     * @return
     */
    public final Map<String, Object> runSingleQuery(String sql, Object... args) {
        return runSingleQuery(new SimpleRowConverter(), sql, args);
    }

    /**
     * Run an update operation and return the number of affected rows.
     * 
     * @param updateStatement Update statement which may contain parameter markers.
     * @param args Arguments for parameter markers in updateStatement, if any.
     * @return The number of affected rows, or -1 if an error occurred.
     */
    public final int runUpdate(String updateStatement, Object... args) {

        int affectedRows;
        Connection connection = null;
        PreparedStatement ps = null;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(updateStatement);

            // do we need a special treatment for NULL values here?
            // if you should stumble across this comment while debugging,
            // the answer is likely: yes, we do!
            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }

            affectedRows = ps.executeUpdate();

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            affectedRows = -1;
        } finally {
            close(connection, ps);
        }

        return affectedRows;

    }
    
    /**
     * 
     * @param updateStatement
     * @param args
     * @return
     */
    public final int runUpdate(String updateStatement, List<Object> args) {
        return runUpdate(updateStatement, args.toArray());
    }

    /**
     * Run an update operation and return the insert ID.
     * 
     * @param updateStatement Update statement which may contain parameter markers.
     * @param args Arguments for parameter markers in updateStatement, if any.
     * @return The generated ID, or 0 if no row was inserted, or -1 if an error occurred.
     */
    public final int runUpdateReturnId(String updateStatement, Object... args) {

        int generatedId;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(updateStatement, Statement.RETURN_GENERATED_KEYS);

            for (int i = 0; i < args.length; i++) {
                ps.setObject(i + 1, args[i]);
            }

            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getInt(1);
            } else {
                generatedId = 0;
            }

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
            generatedId = -1;
        } finally {
            close(connection, ps, rs);
        }

        return generatedId;

    }

    /**
     * 
     * @param updateStatement
     * @param args
     * @return
     */
    public final int runUpdateReturnId(String updateStatement, List<Object> args) {
        return runUpdateReturnId(updateStatement, args.toArray());
    }

    /**
     * 
     * @param connection
     */
    public static final void close(Connection connection) {
        close(connection, null, null);
    }

    /**
     * Convenient helper method to close database resources.
     * 
     * @param connection
     * @param statement
     */
    public static final void close(Connection connection, Statement statement) {
        close(connection, statement, null);
    }

    public static final void close(Connection connection, ResultSet resultSet) {
        close(connection, null, resultSet);
    }

    /**
     * Convenient helper method to close database resources.
     * 
     * @param connection
     * @param statement
     * @param resultSet
     */
    public static final void close(Connection connection, Statement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                LOGGER.error("error closing ResultSet : " + e.getMessage());
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOGGER.error("error closing Statement : " + e.getMessage());
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.error("error closing Connection : " + e.getMessage());
            }
        }
    }

}