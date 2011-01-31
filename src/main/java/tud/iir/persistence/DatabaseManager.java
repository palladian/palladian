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
 * The {@link DatabaseManager} provides general database specific functionality.
 * 
 * No {@link SQLException} are exposed.
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
    private final Connection getConnection() throws SQLException {
        return ConnectionManager.getInstance().getConnection();
    }

    /**
     * Run a query operation on the database, process the result using a callback.
     * 
     * @param <T> Type of the processed objects.
     * @param callback The callback which is triggered for each result row of the query.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
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
            fillPreparedStatement(ps, args);
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
     * Run a query operation on the database, process the result using a callback.
     * 
     * @param callback The callback which is triggered for each result row of the query.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Number of processed results.
     */
    public final int runQuery(SimpleResultCallback callback, String sql, Object... args) {
        return runQuery(callback, new SimpleRowConverter(), sql, args);
    }

    /**
     * Run a query operation on the database, process the result using a callback.
     * 
     * @param callback The callback which is triggered for each result row of the query.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Number of processed results.
     */
    public final int runQuery(RawResultCallback callback, String sql, Object... args) {
        return runQuery(callback, new NopRowConverter(), sql, args);
    }

    /**
     * Run a query operation on the database, return the result as List.
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return List with results.
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
     * Run a query operation on the database, return the result as Iterator. The underlying Iterator implementation does
     * not allow modifications, so invoking {@link Iterator#remove()} will cause an
     * {@link UnsupportedOperationException}. Database resources used by the implementation are closed, after the last
     * element has been retrieved. If you break the iteration loop, you must manually call
     * {@link ResultIterator#close()}. In general, you should prefer using
     * {@link #runQuery(ResultCallback, RowConverter, String, Object...)},
     * {@link #runQuery(SimpleResultCallback, String, Object...)}, or
     * {@link #runQuery(RawResultCallback, String, Object...)}, which will guarantee closing a database resources.
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Iterator for iterating over results.
     */
    public final <T> ResultIterator<T> runQueryWithIterator(RowConverter<T> converter, String sql, Object... args) {

        @SuppressWarnings("unchecked")
        ResultIterator<T> result = ResultIterator.NULL_ITERATOR;
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

            fillPreparedStatement(ps, args);

            resultSet = ps.executeQuery();
            result = new ResultIterator<T>(connection, ps, resultSet, converter);

        } catch (SQLException e) {
            LOGGER.error(e);
            close(connection, ps, resultSet);
        }

        return result;
    }

    /**
     * Run a query operation on the database, return the result as Iterator. The underlying Iterator implementation does
     * not allow modifications, so invoking {@link Iterator#remove()} will cause an
     * {@link UnsupportedOperationException}. Database resources used by the implementation are closed, after the last
     * element has been retrieved. If you break the iteration loop, you must manually call
     * {@link ResultIterator#close()}. In general, you should prefer using
     * {@link #runQuery(ResultCallback, RowConverter, String, Object...)},
     * {@link #runQuery(SimpleResultCallback, String, Object...)}, or
     * {@link #runQuery(RawResultCallback, String, Object...)}, which will guarantee closing a database resources.
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Iterator for iterating over results.
     */    
    public final <T> ResultIterator<T> runQueryWithIterator(RowConverter<T> converter, String sql, List<Object> args) {
        return runQueryWithIterator(converter, sql, args.toArray());
    }

    /**
     * Run a query operation for a single item in the database.
     * 
     * @param <T> Type of the processed object.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return The <i>first</i> retrieved item for the given query, or <code>null</code> no entry found.
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

    /**
     * Run a query operation for a single item in the database.
     * 
     * @param <T> Type of the processed object.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type.
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return The <i>first</i> retrieved item for the given query, or <code>null</code> no entry found.
     */
    public final <T> T runSingleQuery(RowConverter<T> converter, String sql, List<Object> args) {
        return runSingleQuery(converter, sql, args.toArray());
    }
    
    /**
     * Run a query operation for a single item in the database.
     * 
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return The <i>first</i> retrieved item for the given query as Map representation, or <code>null</code> no entry
     *         found.
     */
    public final Map<String, Object> runSingleQuery(String sql, Object... args) {
        return runSingleQuery(new SimpleRowConverter(), sql, args);
    }
    
    /**
     * Run a query operation for a single item in the database.
     * 
     * @param sql Query statement which may contain parameter markers.
     * @param args (Optional) arguments for parameter markers in query.
     * @return The <i>first</i> retrieved item for the given query as Map representation, or <code>null</code> no entry
     *         found.
     */
    public final Map<String, Object> runSingleQuery(String sql, List<Object> args) {
        return runSingleQuery(new SimpleRowConverter(), sql, args.toArray());
    }

    public final int runCountQuery(String countQuery) {

        final int[] count = new int[]{-1};

        SimpleResultCallback callback = new SimpleResultCallback() {

            @Override
            public void processResult(Map<String, Object> object, int number) {
                count[0] = (Integer) object.get("count");
            }
        };

        runQuery(callback, countQuery);
        return count[0];
    }

    public final boolean entryExists(String sql, Object... args) {
        return runSingleQuery(sql, args) != null;
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
            fillPreparedStatement(ps, args);

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

    public final int[] runBatchUpdate(String updateStatement, BatchDataProvider provider) {

        Connection connection = null;
        PreparedStatement ps = null;
        int[] result = new int[0];

        try {

            connection = getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(updateStatement);

            for (int i = 0; i < provider.getCount(); i++) {
                List<Object> args = provider.getData(i);
                fillPreparedStatement(ps, args);
                ps.addBatch();
            }

            result = ps.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            LOGGER.error(e.getMessage());
        } finally {
            close(connection, ps);
        }

        return result;
    }

    public final int[] runBatchUpdate(String updateStatement, final List<List<Object>> batchArgs) {

        BatchDataProvider provider = new BatchDataProvider() {

            @Override
            public List<Object> getData(int number) {
                List<Object> args = batchArgs.get(number);
                return args;
            }

            @Override
            public int getCount() {
                return batchArgs.size();
            }
        };
        
        return runBatchUpdate(updateStatement, provider);
    }

    /**
     * Run an update operation and return the generated insert ID.
     * 
     * @param updateStatement Update statement which may contain parameter markers.
     * @param args Arguments for parameter markers in updateStatement, if any.
     * @return The generated ID, or 0 if no id was generated, or -1 if an error occurred.
     */
    public final int runUpdateReturnId(String updateStatement, Object... args) {

        int generatedId;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;

        try {

            connection = getConnection();
            ps = connection.prepareStatement(updateStatement, Statement.RETURN_GENERATED_KEYS);
            fillPreparedStatement(ps, args);
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

    // //////////////////////////////////////////////////////////////////////////////
    // Helper methods
    // //////////////////////////////////////////////////////////////////////////////
    
    /**
     * Sets {@link PreparedStatement} parameters based on the supplied arguments.
     * 
     * @param ps
     * @param args
     * @throws SQLException
     */
    private static final void fillPreparedStatement(PreparedStatement ps, Object... args) throws SQLException {

        // do we need a special treatment for NULL values here?
        // if you should stumble across this comment while debugging,
        // the answer is likely: yes, we do!
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }

    /**
     * Sets {@link PreparedStatement} parameters based on the supplied arguments.
     * 
     * @param ps
     * @param args
     * @throws SQLException
     */
    private static final void fillPreparedStatement(PreparedStatement ps, List<Object> args) throws SQLException {
        fillPreparedStatement(ps, args.toArray());
    }

    /**
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * 
     * @param connection
     */
    protected static final void close(Connection connection) {
        close(connection, null, null);
    }

    /**
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * 
     * @param connection
     * @param statement
     */
    protected static final void close(Connection connection, Statement statement) {
        close(connection, statement, null);
    }

    /**
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * 
     * @param connection
     * @param resultSet
     */
    protected static final void close(Connection connection, ResultSet resultSet) {
        close(connection, null, resultSet);
    }

    /**
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * 
     * @param connection
     * @param statement
     * @param resultSet
     */
    protected static final void close(Connection connection, Statement statement, ResultSet resultSet) {
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