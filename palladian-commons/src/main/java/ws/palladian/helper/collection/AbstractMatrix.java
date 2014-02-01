package ws.palladian.helper.collection;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Common {@link Matrix} functionality (override existent methods in subclasses, in case it gives a performance
 * benefit).
 * </p>
 * 
 * @author pk
 * @param <K> key.
 * @param <V> value.
 */
public abstract class AbstractMatrix<K, V> implements Matrix<K, V> {

    /**
     * Iterator over a {@link Matrix} rows.
     * 
     * @author pk
     */
    private final class RowIterator implements Iterator<MatrixEntry<K, V>> {
        final Iterator<K> rowNameIterator = getRowKeys().iterator();
        K currentRowKey;

        @Override
        public boolean hasNext() {
            return rowNameIterator.hasNext();
        }

        @Override
        public MatrixEntry<K, V> next() {
            currentRowKey = rowNameIterator.next();
            return new DefaultMatrixEntry<K, V>(getRow(currentRowKey), currentRowKey);
        }

        @Override
        public void remove() {
            removeRow(currentRowKey);
        }
    }

    /**
     * Iterator over a {@link Matrix} columns.
     * 
     * @author pk
     */
    private final class ColumnIterator implements Iterator<MatrixEntry<K, V>> {
        final Iterator<K> columnNameIterator = getColumnKeys().iterator();
        K currentColumnKey;

        @Override
        public boolean hasNext() {
            return columnNameIterator.hasNext();
        }

        @Override
        public MatrixEntry<K, V> next() {
            currentColumnKey = columnNameIterator.next();
            return new DefaultMatrixEntry<K, V>(getColumn(currentColumnKey), currentColumnKey);
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
    public String toString(String separator) {
        Validate.notEmpty(separator, "separator must not be empty");
        
        StringBuilder builder = new StringBuilder();
        boolean headWritten = false;

        // iterate through all rows (y)
        for (MatrixEntry<K, V> row : rows()) {

            // write table head
            if (!headWritten) {
                builder.append(separator);
                for (K xKey : getColumnKeys()) {
                    builder.append(xKey).append(separator);
                }
                builder.append('\n');
                headWritten = true;
            }

            builder.append(row.key()).append(separator);

            // iterate through all columns (x)
            for (K xKey : getColumnKeys()) {
                V value = row.vector().get(xKey);
                if (value != null) {
                    builder.append(value);
                }
                builder.append(separator);
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
    public Iterable<? extends MatrixEntry<K, V>> rows() {
        return new Iterable<MatrixEntry<K, V>>() {
            @Override
            public Iterator<MatrixEntry<K, V>> iterator() {
                return new RowIterator();
            }
        };
    }

    @Override
    public Iterable<? extends MatrixEntry<K, V>> columns() {
        return new Iterable<MatrixEntry<K, V>>() {
            @Override
            public Iterator<MatrixEntry<K, V>> iterator() {
                return new ColumnIterator();
            }
        };
    }

}
