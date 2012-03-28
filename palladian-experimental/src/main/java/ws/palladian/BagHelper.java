package ws.palladian;

import java.util.Iterator;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;

public class BagHelper {
    
    public static <T> T getHighest(Bag<T> bag) {
        int highest = 0;
        T result = null;
        Iterator<T> iterator = bag.uniqueSet().iterator();
        while (iterator.hasNext()) {
            T temp = iterator.next();
            int current = bag.getCount(temp);
            if (current > highest) {
                result = temp;
                highest = current;
            }
        }
        return result;
    }
    
    public static <T> Bag<T> getHighest(Bag<T> bag, int items) {
        Bag<T> temp = new HashBag<T>(bag);
        Bag<T> ret = new HashBag<T>();
        for (int i = 0; i < items; i++) {
            T highest = getHighest(temp);
            if (highest == null) {
                break;
            }
            ret.add(highest, temp.getCount(highest));
            temp.remove(highest);
        }
        return ret;
    }

}
