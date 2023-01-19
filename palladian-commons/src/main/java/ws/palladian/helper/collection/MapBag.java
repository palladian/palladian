package ws.palladian.helper.collection;

import java.util.*;
import java.util.Map.Entry;

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
 */
public class MapBag {

    /** Each item is attached to one bag with a certain id. */
    private Map<String, Integer> map;

    /** We store for each bag what the original key was. */
    private Map<Integer, String> mapKeys;

    public MapBag() {
        map = new LinkedHashMap<>();
        mapKeys = new HashMap<>();
    }

    public Set<String> getAllBagEntries() {
        return map.keySet();
    }

    public void newBag(String bagKey) {
        int size = map.values().size();
        map.put(bagKey, size);
        mapKeys.put(size, bagKey);
    }

    public Set<String> getBag(String bagEntry) {
        Integer bagId = map.get(bagEntry);

        Set<String> bagEntries = new HashSet<>();
        for (Entry<String, Integer> entry : map.entrySet()) {
            if (Objects.equals(entry.getValue(), bagId)) {
                bagEntries.add(entry.getKey());
            }
        }

        return bagEntries;
    }

    public String getBagKey(String bagEntry) {
        Integer bagId = map.get(bagEntry);
        return mapKeys.get(bagId);
    }

    public Collection<String> getBagKeys() {
        return mapKeys.values();
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
        while (size > 1) {
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
            mapKeys.put(bagId, bagKey);
        }

        map.put(bagKey, bagId);
        map.put(bagValue, bagId);
    }

}
