package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

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
public class CountMatrix<K> implements Matrix<K, Integer>, Serializable {

    /** The serial version id. */
    private static final long serialVersionUID = -3624991964111312886L;
    
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
     * Get the count of the specified cell.
     * </p>
     * 
     * @param x The column, not <code>null</code>.
     * @param y The row, not <code>null</code>.
     * @return The count fo the specified cell.
     */
    // FIXME -- this should overwrite get(K, K)
    public int getCount(K x, K y) {
        Validate.notNull(x, "x must not be null");
        Validate.notNull(y, "y must not be null");

        Integer count = get(x, y);
        if (count == null) {
            return 0;
        }
        return count;
    }

    /**
     * <p>
     * Get the sum of all cells in the specified column.
     * </p>
     * 
     * @param x The column, not <code>null</code>.
     * @return The sum of all cells in the column.
     */
    public int getColumnSum(K x) {
        Validate.notNull(x, "x must not be null");

        int sum = 0;
        for (K y : getKeysY()) {
            sum += getCount(x, y);
        }
        return sum;
    }

    /**
     * <p>
     * Get the sum of all cells in the specified row.
     * </p>
     * 
     * @param y The row, not <code>null</code>.
     * @return The sum of all cells in the row.
     */
    public int getRowSum(K y) {
        Validate.notNull(y, "y must not be null");

        int sum = 0;
//        for (K x : getKeysX()) {
//            sum += getCount(x, y);
//        }
        for (Pair<K, Integer> entry : getRow(y)) {
            sum += entry.getValue();
        }
        return sum;
    }

    @Override
    public Integer get(K x, K y) {
        return matrix.get(x, y);
    }

    @Override
    public void set(K x, K y, Integer value) {
        matrix.set(x, y, value);
    }

    @Override
    public Set<K> getKeysX() {
        return matrix.getKeysX();
    }

    @Override
    public Set<K> getKeysY() {
        return matrix.getKeysY();
    }

    @Override
    public int sizeY() {
        return matrix.sizeY();
    }

    @Override
    public int sizeX() {
        return matrix.sizeX();
    }

    @Override
    public String asCsv() {
        return matrix.asCsv();
    }

    @Override
    public void clear() {
        matrix.clear();
    }

    @Override
    public List<Pair<K, Integer>> getRow(K y) {
        return matrix.getRow(y);
    }

    @Override
    public List<Pair<K, Integer>> getColumn(K x) {
        return matrix.getColumn(x);
    };

}
