package ws.palladian.classification;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CollectionHelper.Order;

/**
 * @author pk
 */
final class ImmutableCategoryEntries extends AbstractCategoryEntries {

    /**
     * The map must keep entries sorted by probability, so that the first entry has the highest probability; this way,
     * querying the most probably category is fast.
     */
    private final Map<String, Double> entryMap;

    /**
     * To be created by {@link CategoryEntriesBuilder} only.
     * 
     * @param entryMap The map with the entries.
     */
    ImmutableCategoryEntries(Map<String, Double> entryMap) {
        this.entryMap = CollectionHelper.sortByValue(entryMap, Order.DESCENDING);
    }

    @Override
    public Iterator<Category> iterator() {
        return CollectionHelper.convert(entryMap.entrySet().iterator(), new ImmutableCategory.EntryConverter());
    }

    @Override
    public Category getMostLikely() {
        Entry<String, Double> entry = CollectionHelper.getFirst(entryMap.entrySet());
        return entry != null ? new ImmutableCategory(entry.getKey(), entry.getValue()) : null;
    }

    @Override
    public Category getCategory(String categoryName) {
        Double probability = entryMap.get(categoryName);
        return probability != null ? new ImmutableCategory(categoryName, probability) : null;
    }

    @Override
    public int hashCode() {
        return entryMap.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        ImmutableCategoryEntries other = (ImmutableCategoryEntries)obj;
        return entryMap.equals(other.entryMap);
    }

}
