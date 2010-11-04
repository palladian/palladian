package tud.iir.helper;

import java.util.HashMap;
import java.util.Map.Entry;

public class CountMap extends HashMap<Object, Integer> {

    /**
     * 
     */
    private static final long serialVersionUID = -3624991964111312886L;

    public void increment(Object key) {
        Integer count = get(key);
        int counter = count.intValue();
        counter++;
        put(key, counter);
    }

    @Override
    public Integer get(Object key) {
        Integer count = super.get(key);

        if (count == null) {
            count = 0;
        }

        return count;
    }

    /**
     * Returns the sum of all counts in the CountMap. Where in contrast, {@link #size()} returns the number of
     * <i>unique</i> items in the CountMap.
     * 
     * @return
     */
    public int totalSize() {
        int totalSize = 0;
        for (Entry<Object, Integer> entry : this.entrySet()) {
            totalSize += entry.getValue();
        }
        return totalSize;
    }

}
