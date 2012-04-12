package ws.palladian.persistence;

import java.io.Closeable;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.apache.log4j.Logger;

/**
 * <p>
 * This class enables iterations of database results. Database resources are kept open while iterating, until the whole
 * result has been iterated through. If you abort iterating before the whole iteration, you <b>must</b> call
 * {@link #close()}, elsewise resources will leak.
 * </p>
 * 
 * @param <T> Type of the processed objects.
 * @author Philipp Katz
 */
public class ResultIterator<T> implements Iterator<T>, Closeable {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ResultIterator.class);

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static final class NullIterator extends ResultIterator {

        public NullIterator() {
            super(null, null, null, null);
        }

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new NoSuchElementException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    static final NullIterator NULL_ITERATOR = new NullIterator();

    private final Connection connection;
    private final Statement statement;
    private final ResultSet resultSet;

    private final RowConverter<T> rowConverter;

    /** Reference to the next item which can be retrieved via next(). */
    private T next = null;

    ResultIterator(Connection connection, Statement statement, ResultSet resultSet, RowConverter<T> rowConverter) {
        this.connection = connection;
        this.statement = statement;
        this.resultSet = resultSet;
        this.rowConverter = rowConverter;
    }

    @Override
    public boolean hasNext() {
        boolean hasNext = true;
        try {
            if (next == null) {
                if (resultSet.next()) {
                    next = rowConverter.convert(resultSet);
                } else {
                    close();
                    hasNext = false;
                }
            }
        } catch (SQLException e) {
            LOGGER.error(e);
        }
        return hasNext;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        try {
            return next;
        } finally {
            // set the reference to null, so that the next entry is retrieved by hasNext().
            next = null;
        }
    }

    @Override
    public void remove() {
        // we do not allow modifications.
        throw new UnsupportedOperationException();
    }

    @Override
    public void close() {
        LOGGER.trace("closing ...");
        DatabaseManager.close(connection, statement, resultSet);
    }

}
