package ws.palladian.helper.collection;

import java.util.HashMap;

// FIXME make this generic
public class CountMap2D extends HashMap<String, HashMap<String, Integer>> {

    /** The serial version id. */
    private static final long serialVersionUID = -3624991964111312886L;

    /**
     * Increment the entry with the key by one.
     * 
     * @param key The key of the value that should be incremented.
     */
    public void increment(String x, String y) {
        HashMap<String, Integer> row = get(x);
        if (row == null) {
            row = new HashMap<String, Integer>();
            put(x, row);
        }
        
        Integer integer = row.get(y);
        if (integer == null) {
            integer = new Integer(0);
        }
        int counter = integer.intValue();
        counter++;
        row.put(y, counter);
    }


}
