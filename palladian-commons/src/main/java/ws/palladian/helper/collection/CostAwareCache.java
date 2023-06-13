package ws.palladian.helper.collection;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Cache a fixed number of entries based on a cost value. For example, we want to save memory and only save the top 100 entries that take longest to compute.
 */
public class CostAwareCache<S, T> implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    private final Map<S, CostEntry<S, T>> cacheMap;
    private final TreeMap<Integer, CostEntry<S, T>> sortedCostMap;

    private final int cacheSize;

    public CostAwareCache(int cacheSize) {
        cacheMap = new HashMap<>(cacheSize);
        sortedCostMap = new TreeMap<>();
        this.cacheSize = cacheSize;
    }

    public boolean tryAdd(int cost, S key, T value) {
        if (sortedCostMap.size() < cacheSize) {
            CostEntry<S, T> entry = new CostEntry<>(key, value, cost);
            sortedCostMap.put(entry.getCost(), entry);
            cacheMap.put(entry.getKey(), entry);
            return true;
        }

        // see whether the cost of the entry is high enough to remove another entry
        Map.Entry<Integer, CostEntry<S, T>> integerCostEntryEntry = sortedCostMap.firstEntry();
        CostEntry<S, T> lowestCostEntry = integerCostEntryEntry.getValue();
        if (lowestCostEntry.getCost() < cost) {
            sortedCostMap.pollFirstEntry();
            cacheMap.remove(lowestCostEntry.getKey());
            CostEntry<S, T> entry = new CostEntry<>(key, value, cost);
            sortedCostMap.put(cost, entry);
            cacheMap.put(entry.getKey(), entry);
            return true;
        }

        return false;
    }

    public boolean tryAdd(CostEntry<S, T> entry) {
        return tryAdd(entry.getCost(), entry.getKey(), entry.getValue());
    }

    public T tryGet(S key) {
        CostEntry<S, T> cachedCostEntry = cacheMap.get(key);
        if (cachedCostEntry != null) {
            return cachedCostEntry.getValue();
        }

        return null;
    }

    public Set<Map.Entry<Integer, CostEntry<S, T>>> entries() {
        return sortedCostMap.entrySet();
    }

    public int size() {
        return cacheMap.size();
    }

    public CostEntry<S, T> getFirst() {
        return sortedCostMap.firstEntry().getValue();
    }

    public CostEntry<S, T> getLast() {
        return sortedCostMap.lastEntry().getValue();
    }
}
