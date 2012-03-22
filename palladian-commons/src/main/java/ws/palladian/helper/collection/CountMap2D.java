package ws.palladian.helper.collection;

import java.util.HashMap;
import java.util.Map;

// FIXME make this generic
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
 * 
 */
public class CountMap2D extends HashMap<String, HashMap<String, Integer>> {

    /** The serial version id. */
    private static final long serialVersionUID = -3624991964111312886L;

    /**
     * Increment the entry with the key by one.
     * 
     * @param key The key of the value that should be incremented.
     */
    public void increment(String x, String y) {
        HashMap<String, Integer> row = get(y);
        if (row == null) {
            row = new HashMap<String, Integer>();
            put(y, row);
        }
        
        Integer integer = row.get(x);
        if (integer == null) {
            integer = new Integer(0);
        }
        int counter = integer.intValue();
        counter++;
        row.put(x, counter);
    }

    public int getCount(String x, String y) {
        HashMap<String, Integer> row = get(y);
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

    public int getColumnSum(String x) {
        int sum = 0;

        for (Map<String, Integer> entry : this.values()) {
            for (java.util.Map.Entry<String, Integer> columnEntry : entry.entrySet()) {
                if (columnEntry.getKey().equalsIgnoreCase(x)) {
                    sum += columnEntry.getValue();
                }
            }
        }

        return sum;
    }

    public int getRowSum(String y) {
        int sum = 0;

        Map<String, Integer> row = get(y);
        if (row != null) {
            for (Integer v : row.values()) {
                sum += v;
            }
        }

        return sum;
    }

}
