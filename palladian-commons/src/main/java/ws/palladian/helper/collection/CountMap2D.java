package ws.palladian.helper.collection;

import ws.palladian.helper.math.Matrix;

/**
 * x is columns
 * y is rows
 * 
 * <pre>
 *     | x1 | xn
 * ---------------
 * y1  |    |
 * yn  |    |
 * </pre>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public class CountMap2D<K> extends Matrix<K, Integer> {

    /** The serial version id. */
    private static final long serialVersionUID = -3624991964111312886L;

    /**
     * <p>
     * Shortcut method instead of constructor which allows omitting the type parameter.
     * </p>
     * 
     * @return
     */
    public static <T> CountMap2D<T> create() {
        return new CountMap2D<T>();
    }

    /**
     * Increment the entry with the key by one.
     * 
     * @param key The key of the value that should be incremented.
     */
    public void increment(K x, K y) {
        increment(x, y, 1);
    }

    public void increment(K x, K y, int value) {
        Integer count = get(x, y);
        if (count == null) {
            count = 0;
        }
        count += value;
        set(x, y, count);
    }

    public int getCount(K x, K y) {
        Integer count = get(x,y);
        if (count == null) {
            return 0;
        }
        return count;
    }

    public int getColumnSum(K x) {
        int sum = 0;
        for (K y : getKeysY()) {
            sum += getCount(x, y);
        }
        return sum;
    }

    public int getRowSum(K y) {
        int sum = 0;
        for (K x : getKeysX()) {
            sum += getCount(x, y);
        }
        return sum;
    }

}
