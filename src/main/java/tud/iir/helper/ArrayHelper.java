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

    /**
     * Connect two string arrays.
     * 
     * @param array1
     * @param array2
     * @return
     */
    public static String[] concat(String[] array1, String[] array2) {
        String[] helpArray = new String[array1.length + array2.length];
        System.arraycopy(array1, 0, helpArray, 0, array1.length);
        System.arraycopy(array2, 0, helpArray, array1.length, array2.length);

        return helpArray;
    }

}
