package tud.iir.helper;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Helper functions for common (untyped/generic) arrays.
 * 
 * @author Martin Gregor
 * 
 */
public class ArrayHelper {

    /**
     * Removes null objects out of an array.
     * 
     * @param <T>
     * @param array
     * @return
     */
    public static <T> ArrayList<T> removeNullElements(ArrayList<T> array) {
        ArrayList<T> returnArray = new ArrayList<T>();
        Iterator<T> iterator = array.iterator();
        while (iterator.hasNext()) {
            T element = iterator.next();
            if (element != null) {
                returnArray.add(element);
            }
        }
        return returnArray;
    }
}
