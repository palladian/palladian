package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.Set;

import org.apache.commons.lang3.Validate;

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
public class CountMatrix<K> extends AbstractMatrix<K, Integer> implements Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = -3624991964111312886L;
    
    public static interface NumberVector<K> extends Vector<K, Integer> {
        
        /**
         * @return The sum of all values in this {@link Vector}.
         */
        public int getSum();
        
    }

    private final Matrix<K, Integer> matrix;

    /**
     * @param matrix
     */
    public CountMatrix(Matrix<K, Integer> matrix) {
        this.matrix = matrix;
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
        if (count == null) {
            count = 0;
        }
        count += value;
        set(x, y, count);
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

    @Override
    public Integer get(K x, K y) {
        Integer result = matrix.get(x, y);
        return result != null ? result : 0;
    }

    @Override
    public void set(K x, K y, Integer value) {
        matrix.set(x, y, value);
    }

    @Override
    public Set<K> getColumnKeys() {
        return matrix.getColumnKeys();
    }

    @Override
    public Set<K> getRowKeys() {
        return matrix.getRowKeys();
    }

    @Override
    public void clear() {
        matrix.clear();
    }

    @Override
    public NumberVector<K> getRow(final K y) {
        Validate.notNull(y, "y must not be null");
        return new NumberVector<K>() {
            @Override
            public Integer get(K x) {
                return CountMatrix.this.get(x, y);
            }

            @Override
            public int getSum() {
                int sum = 0;
                for (K x : getColumnKeys()) {
                    sum += get(x);
                }
                return sum;
            }
        };
    }

    @Override
    public NumberVector<K> getColumn(final K x) {
        Validate.notNull(x, "x must not be null");
        return new NumberVector<K>() {
            @Override
            public Integer get(K y) {
                return CountMatrix.this.get(x, y);
            }

            @Override
            public int getSum() {
                int sum = 0;
                for (K y : getRowKeys()) {
                    sum += get(y);
                }
                return sum;
            }
        };
    }

    public int getSum() {
        int totalSize = 0;
        for (K y : getRowKeys()) {
            totalSize += getRow(y).getSum();
        }
        return totalSize;
    };

}
