package ws.palladian.helper.collection;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

/**
 * <p>
 * Multiple bags that can be accessed with any key contained in any of the bags.
 * </p>
 * <p>
 * Consider the following two bags: A = [a,b,c] and B = [d,e,f]. You can now get the bag A by saying MapBag.get(c) or
 * MapBag.get(b) for example.
 * </p>
 * <p>
 * NOTE: Bags must be strictly disjoint, that is, A must not contain any elements from B and vice versa.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class MapBag {

    /** Each item is attached to one bag with a certain id. */
    private Map<String, Integer> map;

    public MapBag() {
        map = CollectionHelper.newHashMap();
    }

    public Set<String> getAllBagEntries() {
        return map.keySet();
    }

    public void newBag(String bagKey) {
        map.put(bagKey, map.values().size());
    }

    public Set<String> getBag(String bagEntry) {
        Integer bagId = map.get(bagEntry);

        Set<String> bagEntries = CollectionHelper.newHashSet();
        for (Entry<String, Integer> entry : map.entrySet()) {
            if (entry.getValue() == bagId) {
                bagEntries.add(entry.getKey());
            }
        }

        return bagEntries;
    }

    /**
     * <p>
     * Get another random entry from the bag which is not equal to the given one.
     * </p>
     * 
     * @param bagEntry The bag in which we have to search.
     * @return A different bag entry.
     */
    public String getDifferentBagEntry(String bagEntry) {

        Set<String> bag = getBag(bagEntry);

        int size = bag.size();
        while (true && size > 1) {
            int item = new Random().nextInt(size);
            int i = 0;
            for (String obj : bag) {
                if (i == item && !obj.equals(bagEntry)) {
                    return obj;
                }
                i = i + 1;
            }
        }

        return null;
    }

    public void add(String bagKey, String bagValue) {
        Integer bagId = map.get(bagKey);

        if (bagId == null) {
            // create a new bag
            bagId = map.values().size();
        }

        map.put(bagKey, bagId);
        map.put(bagValue, bagId);
    }

}
