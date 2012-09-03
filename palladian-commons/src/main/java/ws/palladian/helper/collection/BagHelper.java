package ws.palladian.helper.collection;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Helper methods for {@link Bag}s.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class BagHelper {

    private BagHelper() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Get the item from the bag which has the highest count. If multiple items with this count exist, only one is
     * returned.
     * </p>
     * 
     * @param bag The bag, not <code>null</code>.
     * @return The item from the bag with the highest count or if no such item exists, <code>null</code>.
     */
    public static <T> T getHighest(Bag<T> bag) {
        Validate.notNull(bag, "bag must not be null");
        int highest = 0;
        T result = null;
        for (T item : bag.uniqueSet()) {
            int current = bag.getCount(item);
            if (current > highest) {
                result = item;
                highest = current;
            }
        }
        return result;
    }

    /**
     * <p>
     * Get the number specified number of items with the highest count from the bag.
     * </p>
     * 
     * @param bag
     * @param numItems The number of items to return.
     * @return A bag with the top number of items by count.
     */
    public static <T> Bag<T> getHighest(Bag<T> bag, int numItems) {
        Validate.notNull(bag, "bag must not be null");
        Validate.validState(numItems > 0, "numItems must be greater than 0");
        Bag<T> temp = new HashBag<T>(bag);
        Bag<T> result = new HashBag<T>();
        for (int i = 0; i < numItems; i++) {
            T highest = getHighest(temp);
            if (highest == null) {
                break;
            }
            result.add(highest, temp.getCount(highest));
            temp.remove(highest);
        }
        return result;
    }

}
