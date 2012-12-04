package ws.palladian.helper.collection;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * <p>
 * Multiple bags that can be accessed with any key contained in any of the bags.
 * </p>
 * <p>
 * Consider the following two bags: A = [a,b,c] and B = [d,e,f]. You can now get the bag A by saying MapBag.get(c) or
 * MapBag.get(b) for example.
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

    public void add(String bagKey, String bagValue) {
        Integer bagId = map.get(bagKey);

        if (bagId == null) {
            // create a new bag
            bagId = map.values().size();
        }

        map.put(bagValue, bagId);
    }

}
