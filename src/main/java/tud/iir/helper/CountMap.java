package tud.iir.helper;

import java.util.HashMap;

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

}
