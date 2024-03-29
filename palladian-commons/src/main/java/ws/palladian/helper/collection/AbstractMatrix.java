package ws.palladian.helper.collection;

import org.apache.commons.lang3.Validate;

import java.util.Iterator;
import java.util.LinkedHashSet;

/**
 * <p>
 * Common {@link Matrix} functionality (override existent methods in subclasses, in case it gives a performance
 * benefit, see interface documentation for more information).
 * </p>
 *
 * @param <K> key.
 * @param <V> value.
 * @author Philipp Katz
 */
public abstract class AbstractMatrix<K, V> implements Matrix<K, V> {

    /**
     * Iterator over a {@link Matrix} rows.
     *
     * @author Philipp Katz
     */
    private final class RowIterator implements Iterator<MatrixVector<K, V>> {
        final Iterator<K> rowNameIterator = new LinkedHashSet<>(getRowKeys()).iterator();
        K currentRowKey;

        @Override
        public boolean hasNext() {
            return rowNameIterator.hasNext();
        }

        @Override
        public MatrixVector<K, V> next() {
            currentRowKey = rowNameIterator.next();
            return getRow(currentRowKey);
        }

        @Override
        public void remove() {
            removeRow(currentRowKey);
        }
    }

    /**
     * Iterator over a {@link Matrix} columns.
     *
     * @author Philipp Katz
     */
    private final class ColumnIterator implements Iterator<MatrixVector<K, V>> {
        final Iterator<K> columnNameIterator = new LinkedHashSet<>(getColumnKeys()).iterator();
        K currentColumnKey;

        @Override
        public boolean hasNext() {
            return columnNameIterator.hasNext();
        }

        @Override
        public MatrixVector<K, V> next() {
            currentColumnKey = columnNameIterator.next();
            return getColumn(currentColumnKey);
        }

        @Override
        public void remove() {
            removeColumn(currentColumnKey);
        }
    }

    @Override
    public int columnCount() {
        return getColumnKeys().size();
    }

    @Override
    public int rowCount() {
        return getRowKeys().size();
    }

    @Override
    public int size() {
        return columnCount() * rowCount();
    }

    @Override
    public String toString(String separator) {
        Validate.notEmpty(separator, "separator must not be empty");

        StringBuilder builder = new StringBuilder();
        boolean headWritten = false;

        // iterate through all rows (y)
        for (MatrixVector<K, V> row : rows()) {

            // write table head
            if (!headWritten) {
                for (K xKey : getColumnKeys()) {
                    builder.append(separator).append(xKey);
                }
                builder.append('\n');
                headWritten = true;
            }

            builder.append(row.key());

            // iterate through all columns (x)
            for (K xKey : getColumnKeys()) {
                builder.append(separator);
                V value = row.get(xKey);
                if (value != null) {
                    builder.append(value);
                }
            }
            builder.append('\n');
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return toString("\t");
    }

    @Override
    public V get(K x, K y) {
        Vector<K, V> row = getRow(y);
        return row != null ? row.get(x) : null;
    }

    @Override
    public Iterable<? extends MatrixVector<K, V>> rows() {
        return RowIterator::new;
    }

    @Override
    public Iterable<? extends MatrixVector<K, V>> columns() {
        return ColumnIterator::new;
    }

    @Override
    public boolean isCompatible(Matrix<K, V> other) {
        return getRowKeys().equals(other.getRowKeys()) && getColumnKeys().equals(other.getColumnKeys());
    }

}
