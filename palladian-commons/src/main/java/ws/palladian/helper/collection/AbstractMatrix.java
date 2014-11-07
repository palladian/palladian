package ws.palladian.helper.collection;

/**
 * <p>
 * Common {@link Matrix} functionality.
 * </p>
 * 
 * @author pk
 * @param <K> key.
 * @param <V> value.
 */
public abstract class AbstractMatrix<K, V> implements Matrix<K, V> {

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        boolean headWritten = false;

        // iterate through all rows (y)
        for (K yKey : getRowKeys()) {

            // write table head
            if (!headWritten) {
                builder.append('\t');
                for (K xKey : getColumnKeys()) {
                    builder.append(xKey).append('\t');
                }
                builder.append('\n');
                headWritten = true;
            }

            builder.append(yKey).append('\t');

            // iterate through all columns (x)
            for (K xKey : getColumnKeys()) {
                builder.append(get(xKey, yKey)).append('\t');
            }
            builder.append('\n');
        }
        return builder.toString();
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
    public String toCsv() {
        return toString().replace("\t", ";");
    }

    @Override
    public Vector<K, V> getRow(final K y) {
        return new Vector<K, V>() {
            @Override
            public V get(K x) {
                return AbstractMatrix.this.get(x, y);
            }
        };
    }

    @Override
    public Vector<K, V> getColumn(final K x) {
        return new Vector<K, V>() {
            @Override
            public V get(K y) {
                return AbstractMatrix.this.get(x, y);
            }
        };
    }

}
