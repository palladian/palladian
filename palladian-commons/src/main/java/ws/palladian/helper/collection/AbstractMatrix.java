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
    public V get(K x, K y) {
        Vector<K, V> row = getRow(y);
        return row != null ? row.get(x) : null;
    }

}
