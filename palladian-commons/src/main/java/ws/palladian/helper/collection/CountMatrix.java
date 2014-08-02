package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.functional.Function;

/**
 * <p>
 * A CountMatrix allows counting of items which are indexed by (x, y) coordinates. It is the two-dimensional variant of
 * the {@link CountMap}.
 * </p>
 * 
 * @param <K> The type of the keys in this CountMatrix.
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class CountMatrix<K> extends MatrixDecorator<K, Integer> implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = -3624991964111312886L;

    private final class EntryConverter implements Function<MatrixVector<K, Integer>, IntegerMatrixVector<K>> {
        @Override
        public IntegerMatrixVector<K> compute(MatrixVector<K, Integer> input) {
            return new IntegerMatrixVector<K>(input);
        }
    }

    /**
     * A {@link MatrixVector} decorator, which returns {@link IntegerVector}s.
     * 
     * @author pk
     * 
     * @param <K>
     */
    public static final class IntegerMatrixVector<K> implements MatrixVector<K, Integer> {

        private final MatrixVector<K, Integer> vector;
        final int sum;

        public IntegerMatrixVector(MatrixVector<K, Integer> vector) {
            this.vector = vector;
            int sum = 0;
            for (VectorEntry<K, Integer> entry : vector) {
                sum += entry.value();
            }
            this.sum = sum;
        }

        /**
         * <p>
         * In contrast to what is stated in the interface, this methods returns zero, in case the specified column does
         * not exist for your convenience.
         * </p>
         */
        @Override
        public Integer get(K k) {
            Integer result = vector.get(k);
            return result != null ? result : 0;
        }

        @Override
        public int size() {
            return vector.size();
        }

        @Override
        public Set<K> keys() {
            return vector.keys();
        }

        @Override
        public Iterator<VectorEntry<K, Integer>> iterator() {
            return vector.iterator();
        }

        @Override
        public K key() {
            return vector.key();
        }

        /**
         * @return The sum of all values in this {@link Vector}.
         */
        public int getSum() {
            return sum;
        }

    }

    public CountMatrix(Matrix<K, Integer> matrix) {
        super(matrix);
    }

    /**
     * <p>
     * Shortcut method instead of constructor which allows omitting the type parameter.
     * </p>
     * 
     * @return A new CountMatrix.
     */
    public static <T> CountMatrix<T> create() {
        return new CountMatrix<T>(new MapMatrix<T, Integer>());
    }

    /**
     * <p>
     * Increment the count of the specified cell by one.
     * </p>
     * 
     * @param x The column, not <code>null</code>.
     * @param y The row, not <code>null</code>.
     */
    public void add(K x, K y) {
        Validate.notNull(x, "x must not be null");
        Validate.notNull(y, "y must not be null");
        add(x, y, 1);
    }

    /**
     * <p>
     * Increment the count of the specified cell by a certain number.
     * </p>
     * 
     * @param x The column, not <code>null</code>.
     * @param y The row, not <code>null</code>.
     * @param value The value to add.
     */
    public void add(K x, K y, int value) {
        Validate.notNull(x, "x must not be null");
        Validate.notNull(y, "y must not be null");
        Integer count = get(x, y);
        set(x, y, count += value);
    }

    /**
     * <p>
     * Same as {@link #get(Object, Object)}, just to be consistent to CountMap's method.
     * </p>
     * 
     * @param x
     * @param y
     * @return
     */
    public int getCount(K x, K y) {
        return get(x, y);
    }

    /**
     * <p>
     * In contrast to what is stated in the interface, this methods returns zero, in case the specified cell does not
     * exist.
     * </p>
     */
    @Override
    public Integer get(K x, K y) {
        Integer result = matrix.get(x, y);
        return result != null ? result : 0;
    }

    @Override
    public Iterable<IntegerMatrixVector<K>> rows() {
        return CollectionHelper.convert(matrix.rows(), new EntryConverter());
    }

    @Override
    public Iterable<IntegerMatrixVector<K>> columns() {
        return CollectionHelper.convert(matrix.columns(), new EntryConverter());
    }

    /**
     * <p>
     * In contrast to what is stated in the interface, this methods returns an empty number vector, in case the
     * specified row does not exist for your convenience.
     * </p>
     */
    @Override
    public IntegerMatrixVector<K> getRow(K y) {
        Validate.notNull(y, "y must not be null");
        MatrixVector<K, Integer> row = matrix.getRow(y);
        return new IntegerMatrixVector<K>(row != null ? row : new NullMatrixVector<K, Integer>(y));
    }

    /**
     * <p>
     * In contrast to what is stated in the interface, this methods returns an empty number vector, in case the
     * specified column does not exist for your convenience.
     * </p>
     */
    @Override
    public IntegerMatrixVector<K> getColumn(K x) {
        Validate.notNull(x, "x must not be null");
        MatrixVector<K, Integer> column = matrix.getColumn(x);
        return new IntegerMatrixVector<K>(column != null ? column : new NullMatrixVector<K, Integer>(x));
    }

    /**
     * @return The sum of all entries in this matrix.
     */
    public int getSum() {
        int totalSize = 0;
        for (IntegerMatrixVector<K> row : rows()) {
            totalSize += row.getSum();
        }
        return totalSize;
    }

}
