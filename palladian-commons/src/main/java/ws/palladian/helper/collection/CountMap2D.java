package ws.palladian.helper.collection;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

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
public class CountMap2D<T> implements Serializable {
    
    // XXX subclass of Matrix?
    
    /** The serial version id. */
    private static final long serialVersionUID = -3624991964111312886L;
    
    private Map<T, Map<T, Integer>> map = new HashMap<T, Map<T, Integer>>(); 
    
    public static <T> CountMap2D<T> create() {
        return new CountMap2D<T>();
    }
    
    private CountMap2D() {
        
    }

    /**
     * Increment the entry with the key by one.
     * 
     * @param key The key of the value that should be incremented.
     */
    public void increment(T x, T y) {
        increment(x, y, 1);
    }

    public void increment(T x, T y, int value) {
        Map<T, Integer> row = map.get(y);
        if (row == null) {
            row = new HashMap<T, Integer>();
            map.put(y, row);
        }
        
        Integer integer = row.get(x);
        if (integer == null) {
            integer = new Integer(0);
        }
        int counter = integer.intValue();
        counter+=value;
        row.put(x, counter);
    }

    public int getCount(T x, T y) {
        Map<T, Integer> row = map.get(y);
        if (row == null) {
            return 0;
        }

        Integer integer = row.get(x);
        if (integer == null) {
            return 0;
        }

        int counter = integer.intValue();

        return counter;
    }

    public int getColumnSum(T x) {
        int sum = 0;

        for (Map<T, Integer> entry : map.values()) {
            for (java.util.Map.Entry<T, Integer> columnEntry : entry.entrySet()) {
                if (columnEntry.getKey().equals(x)) {
                    sum += columnEntry.getValue();
                }
            }
        }

        return sum;
    }

    public int getRowSum(T y) {
        int sum = 0;

        Map<T, Integer> row = map.get(y);
        if (row != null) {
            for (Integer v : row.values()) {
                sum += v;
            }
        }

        return sum;
    }

    public Map<T, Integer> get(T x) {
        return map.get(x);
    }

    public Set<Entry<T, Map<T, Integer>>> entrySet() {
        return map.entrySet();
    }
    
    @Override
    public String toString() {
        return map.toString();
    }

}
