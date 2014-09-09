package ws.palladian.persistence;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.sql.DataSource;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * <p>
 * The {@link DatabaseManager} provides general database specific functionality. This implementation aims on wrapping
 * all ugly SQL specific details like {@link SQLException}s and automatically closes resources for you where applicable.
 * If you need to create your own application specific persistence layer, you may create your own subclass.
 * </p>
 * 
 * <p>
 * Instances of the DatabaseManager or its subclasses are created using the {@link DatabaseManagerFactory}, which takes
 * care of injecting the {@link DataSource}, which provides database connections.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 * @author Klemens Muthmann
 */
public class DatabaseManager {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatabaseManager.class);

    /**
     * The {@link DataSource} providing Connection to the underlying database.
     */
    private final DataSource dataSource;

    /**
     * <p>
     * Creates a new {@code DatabaseManager} which connects to the database via the specified {@link DataSource}. The
     * constructor is not exposed since new objects of this type must be constructed using the
     * {@link DatabaseManagerFactory}.
     * </p>
     * 
     * @param dataSource The {@link DataSource}, which provides JDBC connections. Must not be <code>null</code>.
     */
    protected DatabaseManager(DataSource dataSource) {
        Validate.notNull(dataSource, "dataSource must not be null");
        this.dataSource = dataSource;
    }

    /**
     * <p>
     * Get a {@link Connection} from the {@link BoneCpDataSourceFactory}. If you use this method, e.g. in your subclass, it's
     * your responsibility to close all database resources after work has been done. This can be done conveniently by
     * using one of the various close methods offered by this class.
     * </p>
     * 
     * @return A connection to the database obtained from the {@link DataSource}.
     * @throws SQLException In case, obtaining the connection fails.
     */
    protected final Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * <p>
     * Check, whether an item for the specified query exists.
     * </p>
     * 
     * @param sql Query statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args Arguments for parameter markers in query, or empty List, not <code>null</code>.
     * @return <code>true</code> if at least on item exists, <code>false</code> otherwise.
     */
    public final boolean entryExists(String sql, List<? extends Object> args) {
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return entryExists(sql, args.toArray());
    }

    /**
     * <p>
     * Check, whether an item for the specified query exists.
     * </p>
     * 
     * @param sql Query statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args (Optional) arguments for parameter markers in query.
     * @return <code>true</code> if at least on item exists, <code>false</code> otherwise.
     */
    public final boolean entryExists(String sql, Object... args) {
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runSingleQuery(NopRowConverter.INSTANCE, sql, args) != null;
    }

    /**
     * <p>
     * Run a batch insertion. The generated ID for each inserted object is provided via the {@link BatchDataProvider}.
     * For each successful insertion, {@link BatchDataProvider#insertedItem(int, int)} is triggered to allow access to
     * the generated ID.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers, not <code>null</code> or empty.
     * @param provider A callback, which provides the necessary data for the insertion, not <code>null</code>.
     * @return The number of inserted rows.
     */
    public final int runBatchInsert(String sql, BatchDataProvider provider) {
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(provider, "provider must not be null");

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int affectedRows = 0;
        List<? extends Object> data = null;

        try {

            connection = getConnection();
            connection.setAutoCommit(false);

            ps = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);

            for (int i = 0; i < provider.getCount(); i++) {
                data = provider.getData(i);
                fillPreparedStatement(ps, data);
                ps.executeUpdate();

                rs = ps.getGeneratedKeys();
                if (rs.next()) {
                    provider.insertedItem(i, rs.getInt(1));
                } else if (ps.getUpdateCount() == 1) {
                    // no ID generated
                    provider.insertedItem(i, -1);
                }
                affectedRows++;
            }

            connection.commit();
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            rollback(connection);
            affectedRows = 0;
            Object[] args = null;
            if (data != null) {
                args = data.toArray();
            }
            logError(e, sql, args);
        } finally {
            close(connection, ps, rs);
        }

        return affectedRows;
    }

    /**
     * <p>
     * Run a batch insertion and return the generated insert IDs.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers, not <code>null</code> or empty.
     * @param batchArgs List of arguments for the batch insertion. Arguments are supplied parameter lists. Not
     *            <code>null</code>.
     * @return Array with generated IDs for the data provided by the provider. This means, the size of the returned
     *         array reflects the number of batch insertions. If a specific row was not inserted, the array will contain
     *         a 0 value.
     */
    public final int[] runBatchInsertReturnIds(String sql, final List<List<Object>> batchArgs) {
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(batchArgs, "batchArgs must not be null");

        final int[] result = new int[batchArgs.size()];
        Arrays.fill(result, 0);

        BatchDataProvider provider = new BatchDataProvider() {

            @Override
            public int getCount() {
                return batchArgs.size();
            }

            @Override
            public List<? extends Object> getData(int number) {
                return batchArgs.get(number);
            }

            @Override
            public void insertedItem(int number, int generatedId) {
                result[number] = generatedId;
            }
        };

        runBatchInsert(sql, provider);
        return result;
    }

    /**
     * <p>
     * Run a batch update.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers, not <code>null</code> or empty.
     * @param provider A callback, which provides the necessary data for the update, not <code>null</code>.
     * @return An array of update counts for each statement in the batch.
     */
    public final int[] runBatchUpdate(String sql, BatchDataProvider provider) {
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(provider, "provider must not be null");

        Connection connection = null;
        PreparedStatement ps = null;
        int[] result = new int[0];

        try {

            connection = getConnection();
            connection.setAutoCommit(false);
            ps = connection.prepareStatement(sql);

            for (int i = 0; i < provider.getCount(); i++) {
                List<? extends Object> args = provider.getData(i);
                fillPreparedStatement(ps, args);
                ps.addBatch();
            }

            result = ps.executeBatch();
            connection.commit();
            connection.setAutoCommit(true);

        } catch (SQLException e) {
            rollback(connection);
            logError(e, sql);
        } finally {
            close(connection, ps);
        }

        return result;
    }

    /**
     * <p>
     * Run a batch update.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers, not <code>null</code> or empty.
     * @param batchArgs List of arguments for the batch update. Arguments are supplied parameter lists. Not
     *            <code>null</code>.
     * @return An array of update counts for each statement in the batch.
     */
    public final int[] runBatchUpdate(String sql, final List<List<Object>> batchArgs) {
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(batchArgs, "batchArgs must not be null");

        BatchDataProvider provider = new BatchDataProvider() {

            @Override
            public int getCount() {
                return batchArgs.size();
            }

            @Override
            public List<? extends Object> getData(int number) {
                return batchArgs.get(number);
            }

            @Override
            public void insertedItem(int number, int generatedId) {
                // no op.
            }
        };

        return runBatchUpdate(sql, provider);
    }

    /**
     * <p>
     * Run a query which only returns a single {@link Integer} result (i.e. one row, one column). This is handy for
     * aggregate queries, like <code>COUNT</code>, <code>SUM</code>, <code>AVG</code>, <code>MAX</code>,
     * <code>MIN</code>. Example for such a query: <code>SELECT COUNT(*) FROM feeds WHERE id > 342</code>.
     * </p>
     * 
     * @param sql The query string for the aggregated integer result, not <code>null</code> or empty.
     * @return The result of the query, or <code>null</code> if no result.
     */
    public final Integer runAggregateQuery(String sql) {
        Validate.notEmpty(sql, "sql must not be empty");
        return runSingleQuery(OneColumnRowConverter.INTEGER, sql);
    }

    /**
     * <p>
     * Run an insert operation and return the generated insert ID.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args Arguments for parameter markers in update statement, or empty List, not <code>null</code>.
     * @return The generated ID, or 0 if no id was generated, or -1 if an error occurred.
     */
    public final int runInsertReturnId(String sql, List<? extends Object> args) {
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runInsertReturnId(null, sql, args.toArray());
    }

    /**
     * <p>
     * Run an insert operation and return the generated insert ID.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args (Optional) arguments for parameter markers in update statement.
     * @return The generated ID, or 0 if no id was generated, or -1 if an error occurred.
     */
    public final int runInsertReturnId(String sql, Object... args) {
        return runInsertReturnId(null, sql, args);
    }

    /**
     * <p>
     * Run an insert operation and return the generated insert ID. <b>NOTE</b>: If a connection is given, you
     * <b>must</b> close it yourself or reuse it.
     * </p>
     * 
     * @param connection The connection to use for the update or <code>null</code> if a new connection should be
     *            retrieved from the pool.
     * @param sql Update statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args (Optional) arguments for parameter markers in update statement.
     * @return The generated ID, or 0 if no id was generated, or -1 if an error occurred.
     */
    public final int runInsertReturnId(Connection connection, String sql, Object... args) {
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runInsertReturnId(connection, new BasicQuery(sql, args));
    }

    /**
     * <p>
     * Run an insert operation and return the generated insert ID.
     * </p>
     * 
     * @param query The query including the (optional) arguments, not <code>null</code>.
     * @return The generated ID, or 0 if no id was generated, or -1 if an error occurred.
     */
    public final int runInsertReturnId(Query query) {
        return runInsertReturnId(null, query);
    }

    /**
     * <p>
     * Run an insert operation and return the generated insert ID. <b>NOTE</b>: If a connection is given, you
     * <b>must</b> close it yourself or reuse it.
     * </p>
     * 
     * @param connection The connection to use for the update or <code>null</code> if a new connection should be
     *            retrieved from the pool.
     * @param query The query including the (optional) arguments, not <code>null</code>.
     * @return The generated ID, or 0 if no id was generated, or -1 if an error occurred.
     */
    public final int runInsertReturnId(Connection connection, Query query) {
        int generatedId;
        PreparedStatement ps = null;
        ResultSet rs = null;

        boolean closeConnection = false;
        if (connection == null) {
            closeConnection = true;
        }

        try {

            if (connection == null) {
                connection = getConnection();
            }

            // connection = getConnection();
            ps = connection.prepareStatement(query.getSql(), Statement.RETURN_GENERATED_KEYS);
            fillPreparedStatement(ps, query.getArgs());
            ps.executeUpdate();

            rs = ps.getGeneratedKeys();
            if (rs.next()) {
                generatedId = rs.getInt(1);
            } else {
                generatedId = 0;
            }


        } catch (SQLException e) {
            logError(e, query.getSql(), query.getArgs());
            generatedId = -1;
        } finally {
            if (closeConnection) {
                close(connection, ps, rs);
            } else {
                close(null, ps, rs);
            }
        }

        return generatedId;
    }

    /**
     * @deprecated This should be done using {@link #runSingleQuery(RowConverter, String, Object...)} supplying a
     *             {@link RowConverter} returning an Object[]. There is no need to explicitly specify the number of
     *             entries.
     */
    @Deprecated
    public final Object[] runOneResultLineQuery(String query, final int entries, Object... args) {

        final Object[] resultEntries = new Object[entries];

        ResultSetCallback callback = new ResultSetCallback() {

            @Override
            public void processResult(ResultSet resultSet, int number) throws SQLException {
                for (int i = 1; i <= entries; i++) {
                    resultEntries[i - 1] = resultSet.getObject(i);
                }

            }
        };

        runQuery(callback, query, args);

        return resultEntries;
    }

    /**
     * <p>
     * Run a query operation on the database, process the result using a callback.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param callback The callback which is triggered for each result row of the query, not <code>null</code>.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param sql Query statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Number of processed results.
     */
    public final <T> int runQuery(ResultCallback<T> callback, RowConverter<T> converter, String sql, Object... args) {
        Validate.notNull(callback, "callback must not be null");
        Validate.notNull(converter, "converter must not be null");
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runQuery(callback, converter, new BasicQuery(sql, args));
    }

    /**
     * <p>
     * Run a query operation on the database, process the result using a callback.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param callback The callback which is triggered for each result row of the query, not <code>null</code>.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param query The query including the (optional) arguments, not <code>null</code>.
     * @return Number of processed results.
     */
    public final <T> int runQuery(ResultCallback<T> callback, RowConverter<T> converter, Query query) {
        Validate.notNull(callback, "callback must not be null");
        Validate.notNull(converter, "converter must not be null");
        Validate.notNull(query, "query must not be null");

        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet rs = null;
        int counter = 0;

        try {
            connection = getConnection();

            ps = connection.prepareStatement(query.getSql());
            fillPreparedStatement(ps, query.getArgs());
            rs = ps.executeQuery();

            while (rs.next() && callback.isLooping()) {
                T item = converter.convert(rs);
                callback.processResult(item, ++counter);
            }

        } catch (SQLException e) {
            logError(e, query.getSql(), query.getArgs());
        } finally {
            close(connection, ps, rs);
        }

        return counter;
    }

    /**
     * <p>
     * Run a query operation on the database, process the result using a callback.
     * </p>
     * 
     * @param callback The callback which is triggered for each result row of the query, not <code>null</code>.
     * @param sql Query statement which may contain parameter markers, nut <code>null</code> or empty.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Number of processed results.
     */
    public final int runQuery(ResultSetCallback callback, String sql, Object... args) {
        Validate.notNull(callback, "callback must not be null");
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runQuery(callback, NopRowConverter.INSTANCE, sql, args);
    }

    /**
     * <p>
     * Run a query operation on the database, return the result as List.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param sql Query statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args Arguments for parameter markers in query, or empty List, not <code>null</code>.
     * @return List with results.
     */
    public final <T> List<T> runQuery(RowConverter<T> converter, String sql, List<? extends Object> args) {
        Validate.notNull(converter, "converter must not be null");
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runQuery(converter, new BasicQuery(sql, args));
    }

    /**
     * <p>
     * Run a query operation on the database, return the result as List.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param sql Query statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args (Optional) arguments for parameter markers in query.
     * @return List with results.
     */
    public final <T> List<T> runQuery(RowConverter<T> converter, String sql, Object... args) {
        Validate.notNull(converter, "converter must not be null");
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runQuery(converter, new BasicQuery(sql, args));
    }

    /**
     * <p>
     * Run a query operation on the database, return the result as List.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param query The query including the (optional) arguments, not <code>null</code>.
     * @return List with results.
     */
    public final <T> List<T> runQuery(RowConverter<T> converter, Query query) {
        Validate.notNull(converter, "converter must not be null");
        Validate.notNull(query, "query must not be null");

        final List<T> result = new ArrayList<T>();

        ResultCallback<T> callback = new ResultCallback<T>() {

            @Override
            public void processResult(T object, int number) {
                result.add(object);
            }

        };

        runQuery(callback, converter, query);
        return result;
    }

    /**
     * <p>
     * Run a query operation on the database, return the result as set.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param sql Query statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Set with results.
     */
    public final <T> Set<T> runDistinctQuery(RowConverter<T> converter, String sql, Object... args) {
        return new HashSet<T>(runQuery(converter, new BasicQuery(sql, args)));
    }

    /**
     * <p>
     * Run a query operation on the database, return the result as set.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param query The query including the (optional) arguments, not <code>null</code>.
     * @return Set with results.
     */
    public final <T> Set<T> runDistinctQuery(RowConverter<T> converter, Query query) {
        return new HashSet<T>(runQuery(converter, query));
    }

    /**
     * <p>
     * Run a query operation on the database, return the result as Iterator. The underlying Iterator implementation does
     * not allow modifications, so invoking {@link ResultIterator#remove()} will cause an
     * {@link UnsupportedOperationException}. Database resources used by the implementation are closed, after the last
     * element has been retrieved. If you break the iteration loop, you <b>must</b> manually call
     * {@link ResultIterator#close()}. In general, you should prefer using
     * {@link #runQuery(ResultCallback, RowConverter, String, Object...)}, or
     * {@link #runQuery(ResultSetCallback, String, Object...)}, which will guarantee closing all database resources.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param sql Query statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args Arguments for parameter markers in query, or empty List, not <code>null</code>.
     * @return Iterator for iterating over results.
     */
    public final <T> ResultIterator<T> runQueryWithIterator(RowConverter<T> converter, String sql,
            List<? extends Object> args) {
        Validate.notNull(converter, "converter must not be null");
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runQueryWithIterator(converter, new BasicQuery(sql, args));
    }

    /**
     * <p>
     * Run a query operation on the database, return the result as Iterator. The underlying Iterator implementation does
     * not allow modifications, so invoking {@link ResultIterator#remove()} will cause an
     * {@link UnsupportedOperationException}. Database resources used by the implementation are closed, after the last
     * element has been retrieved. If you break the iteration loop, you <b>must</b> manually call
     * {@link ResultIterator#close()}. In general, you should prefer using
     * {@link #runQuery(ResultCallback, RowConverter, String, Object...)}, or
     * {@link #runQuery(ResultSetCallback, String, Object...)}, which will guarantee closing all database resources.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param sql Query statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args (Optional) arguments for parameter markers in query.
     * @return Iterator for iterating over results.
     */
    public final <T> ResultIterator<T> runQueryWithIterator(RowConverter<T> converter, String sql, Object... args) {
        Validate.notNull(converter, "converter must not be null");
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runQueryWithIterator(converter, new BasicQuery(sql, args));
    }

    /**
     * <p>
     * Run a query operation on the database, return the result as Iterator. The underlying Iterator implementation does
     * not allow modifications, so invoking {@link ResultIterator#remove()} will cause an
     * {@link UnsupportedOperationException}. Database resources used by the implementation are closed, after the last
     * element has been retrieved. If you break the iteration loop, you <b>must</b> manually call
     * {@link ResultIterator#close()}. In general, you should prefer using
     * {@link #runQuery(ResultCallback, RowConverter, String, Object...)}, or
     * {@link #runQuery(ResultSetCallback, String, Object...)}, which will guarantee closing all database resources.
     * </p>
     * 
     * @param <T> Type of the processed objects.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param query The query including the (optional) arguments, not <code>null</code>.
     * @return Iterator for iterating over results.
     */
    public final <T> ResultIterator<T> runQueryWithIterator(RowConverter<T> converter, Query query) {
        Validate.notNull(converter, "converter must not be null");
        Validate.notNull(query, "query must not be null");

        @SuppressWarnings("unchecked")
        ResultIterator<T> result = ResultIterator.NULL_ITERATOR;
        Connection connection = null;
        PreparedStatement ps = null;
        ResultSet resultSet = null;

        try {

            connection = getConnection();

            // do not buffer the whole ResultSet in memory, but use streaming to save memory; see:
            // http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-implementation-notes.html
            ps = connection.prepareStatement(query.getSql(), ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
            try {
                ps.setFetchSize(Integer.MIN_VALUE);
            } catch (SQLException e) {
                LOGGER.warn("Exception at Statement#setFetchSize(Integer.MIN_VALUE). This is caused, when the database is not MySQL.");
            }

            fillPreparedStatement(ps, query.getArgs());

            resultSet = ps.executeQuery();
            result = new ResultIterator<T>(connection, ps, resultSet, converter);

        } catch (SQLException e) {
            logError(e, query.getSql(), query.getArgs());
            close(connection, ps, resultSet);
        }

        return result;
    }

    /**
     * <p>
     * Run a query operation for a single item in the database.
     * </p>
     * 
     * @param <T> Type of the processed object.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param sql Query statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args Arguments for parameter markers in query, or empty List, not <code>null</code>.
     * @return The <i>first</i> retrieved item for the given query, or <code>null</code> no item found.
     */
    public final <T> T runSingleQuery(RowConverter<T> converter, String sql, List<? extends Object> args) {
        Validate.notNull(converter, "converter must not be null");
        Validate.notNull(sql, "sql must not be null");
        Validate.notNull(args, "args must not be null");
        return runSingleQuery(converter, new BasicQuery(sql, args));
    }

    /**
     * <p>
     * Run a query operation for a single item in the database.
     * </p>
     * 
     * @param <T> Type of the processed object.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param sql Query statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args (Optional) arguments for parameter markers in query, not <code>null</code>.
     * @return The <i>first</i> retrieved item for the given query, or <code>null</code> no item found.
     */
    public final <T> T runSingleQuery(RowConverter<T> converter, String sql, Object... args) {
        Validate.notNull(converter, "converter must not be null");
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runSingleQuery(converter, new BasicQuery(sql, args));
    }

    /**
     * <p>
     * Run a query operation for a single item in the database.
     * </p>
     * 
     * @param <T> Type of the processed object.
     * @param converter Converter for transforming the {@link ResultSet} to the desired type, not <code>null</code>.
     * @param query The query including the (optional) arguments, not <code>null</code>.
     * @return The <i>first</i> retrieved item for the given query, or <code>null</code> no item found.
     */
    @SuppressWarnings("unchecked")
    public final <T> T runSingleQuery(RowConverter<T> converter, Query query) {
        final Object[] result = new Object[1];

        ResultCallback<T> callback = new ResultCallback<T>() {

            @Override
            public void processResult(T object, int number) {
                result[0] = object;
                breakLoop();
            }
        };

        runQuery(callback, converter, query.getSql(), query.getArgs());
        return (T)result[0];
    }

    /**
     * <p>
     * Run an update operation and return the number of affected rows.
     * </p>
     * 
     * @param sql Update statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args Arguments for parameter markers in update statement, or empty List, not <code>null</code>.
     * @return The number of affected rows, or -1 if an error occurred.
     */
    public final int runUpdate(String sql, List<? extends Object> args) {
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runUpdate(new BasicQuery(sql, args));
    }


    public final int runUpdate(String sql, Object... args) {
        return runUpdate(null, sql, args);
    }

    /**
     * <p>
     * Run an update operation and return the number of affected rows.
     * </p>
     * <p>
     * NOTE: If a connection is given, you <b>must</b> close it yourself or reuse it.
     * </p>
     * 
     * @param connection The connection to use for the update or <code>null</code> if a new connection should be
     *            retrieved from the pool.
     * @param sql Update statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args Arguments for parameter markers in update statement, or empty List, not <code>null</code>.
     * @return The number of affected rows, or -1 if an error occurred.
     */
    public final int runUpdate(Connection connection, String sql, List<? extends Object> args) {
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runUpdate(connection, new BasicQuery(sql, args));
    }

    /**
     * <p>
     * Run an update operation and return the number of affected rows.
     * </p>
     * <p>
     * NOTE: If a connection is given, you <b>must</b> close it yourself or reuse it.
     * </p>
     * 
     * @param connection The connection to use for the update or <code>null</code> if a new connection should be
     *            retrieved from the pool.
     * @param sql Update statement which may contain parameter markers, not <code>null</code> or empty.
     * @param args (Optional) arguments for parameter markers in updateStatement.
     * @return The number of affected rows, or -1 if an error occurred.
     */
    public final int runUpdate(Connection connection, String sql, Object... args) {
        Validate.notEmpty(sql, "sql must not be empty");
        Validate.notNull(args, "args must not be null");
        return runUpdate(connection, new BasicQuery(sql, args));
    }

    public final int runUpdate(Query query) {
        return runUpdate(null, query);
    }

    /**
     * <p>
     * Run an update operation and return the number of affected rows.
     * </p>
     * <p>
     * NOTE: If a connection is given, you <b>must</b> close it yourself or reuse it.
     * </p>
     * 
     * @param connection The connection to use for the update or <code>null</code> if a new connection should be
     *            retrieved from the pool.
     * @param query The query including the (optional) arguments, not <code>null</code>.
     * @return The number of affected rows, or -1 if an error occurred.
     */
    public final int runUpdate(Connection connection, Query query) {
        Validate.notNull(query, "query must not be null");

        int affectedRows;
        PreparedStatement ps = null;
        boolean closeConnection = false;
        if (connection == null) {
            closeConnection = true;
        }

        try {

            if (connection == null) {
                connection = getConnection();
            }
            ps = connection.prepareStatement(query.getSql());
            fillPreparedStatement(ps, query.getArgs());

            affectedRows = ps.executeUpdate();

        } catch (SQLException e) {
            logError(e, query.getSql(), query.getArgs());
            affectedRows = -1;
        } finally {
            if (closeConnection) {
                close(connection, ps);
            } else {
                close(ps);
            }
        }

        return affectedRows;
    }

    // //////////////////////////////////////////////////////////////////////////////
    // Helper methods
    // //////////////////////////////////////////////////////////////////////////////

    /**
     * <p>
     * Log some diagnostics in case of errors. This includes the {@link SQLException} being thrown, the SQL statement
     * and the arguments, if any.
     * </p>
     * 
     * @param exception The exception which occurred, not <code>null</code>.
     * @param sql The executed SQL statement, not <code>null</code>.
     * @param args The arguments for the SQL statement, may be <code>null</code>.
     */
    protected static final void logError(SQLException exception, String sql, Object... args) {
        StringBuilder errorLog = new StringBuilder();
        errorLog.append("Exception " + exception.getMessage() + " when performing SQL \"" + sql + "\"");
        if (args != null && args.length > 0) {
            errorLog.append(" with args \"").append(StringUtils.join(args, ",")).append("\"");
        }
        LOGGER.error(errorLog.toString());
        LOGGER.debug(errorLog.toString(), exception); // only print stack trace in DEBUG mode
    }

    /**
     * <p>
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * </p>
     * 
     * @param connection The {@link Connection}, or <code>null</code>.
     */
    protected static final void close(Connection connection) {
        close(connection, null, null);
    }

    /**
     * <p>
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * </p>
     * 
     * @param resultSet The {@link ResultSet}, or <code>null</code>.
     */
    protected static final void close(ResultSet resultSet) {
        close(null, null, resultSet);
    }

    /**
     * <p>
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * </p>
     * 
     * @param statement The {@link Statement}, or <code>null</code>.
     */
    protected static final void close(Statement statement) {
        close(null, statement, null);
    }

    /**
     * <p>
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * </p>
     * 
     * @param connection The {@link Connection}, or <code>null</code>.
     * @param resultSet The {@link ResultSet}, or <code>null</code>.
     */
    protected static final void close(Connection connection, ResultSet resultSet) {
        close(connection, null, resultSet);
    }

    /**
     * <p>
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * </p>
     * 
     * @param connection The {@link Connection}, or <code>null</code>.
     * @param statement The {@link Statement}, or <code>null</code>.
     */
    protected static final void close(Connection connection, Statement statement) {
        close(connection, statement, null);
    }

    /**
     * <p>
     * Convenience method to close database resources. This method will perform <code>null</code> checking, close
     * resources where applicable and swallow all {@link SQLException}s.
     * </p>
     * 
     * @param connection The {@link Connection}, or <code>null</code>.
     * @param statement The {@link Statement}, or <code>null</code>.
     * @param resultSet The {@link ResultSet}, or <code>null</code>.
     */
    protected static final void close(Connection connection, Statement statement, ResultSet resultSet) {
        if (resultSet != null) {
            try {
                resultSet.close();
            } catch (SQLException e) {
                LOGGER.error("Error closing ResultSet : {}", e.getMessage());
            }
        }
        if (statement != null) {
            try {
                statement.close();
            } catch (SQLException e) {
                LOGGER.error("Error closing Statement : {}", e.getMessage());
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (SQLException e) {
                LOGGER.error("Error closing Connection : {}", e.getMessage());
            }
        }
    }

    /**
     * <p>
     * Sets {@link PreparedStatement} parameters based on the supplied arguments.
     * </p>
     * 
     * @param ps The {@link PreparedStatement} for which to set parameters.
     * @param args {@link List} of parameters to set.
     * @throws SQLException In case setting the parameters failed.
     */
    protected static final void fillPreparedStatement(PreparedStatement ps, List<? extends Object> args)
            throws SQLException {
        fillPreparedStatement(ps, args.toArray());
    }

    /**
     * <p>
     * Sets {@link PreparedStatement} parameters based on the supplied arguments.
     * </p>
     * 
     * @param ps The {@link PreparedStatement} for which to set parameters.
     * @param args The parameters to set.
     * @throws SQLException In case setting the parameters failed.
     */
    protected static final void fillPreparedStatement(PreparedStatement ps, Object... args) throws SQLException {

        // do we need a special treatment for NULL values here?
        // if you should stumble across this comment while debugging,
        // the answer is likely: yes, we do!
        for (int i = 0; i < args.length; i++) {
            ps.setObject(i + 1, args[i]);
        }
    }

    /**
     * <p>
     * Rollback the connection.
     * </p>
     * 
     * @param connection The connection, or <code>null</code>.
     */
    protected static final void rollback(Connection connection) {
        if (connection != null) {
            try {
                connection.rollback();
            } catch (SQLException e) {
                LOGGER.error("Error while rollback: {}", e);
            }
        }
    }

}
