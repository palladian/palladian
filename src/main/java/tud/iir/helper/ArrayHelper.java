package tud.iir.helper;

import java.util.ArrayList;
import java.util.Iterator;

public class ArrayHelper {

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
