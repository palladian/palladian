package ws.palladian.classification;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.math.MathHelper;

/**
 * @author pk
 */
final class ImmutableCategoryEntries implements CategoryEntries {

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
    public Iterator<String> iterator() {
        Set<String> categories = entryMap.keySet();
        return Collections.unmodifiableSet(categories).iterator();
    }

    @Override
    public double getProbability(String categoryName) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
        Double result = entryMap.get(categoryName);
        return result != null ? result : 0;
    }

    @Override
    public String getMostLikelyCategory() {
//        double maxProbability = -1;
//        String maxName = null;
//        for (Entry<String, Double> entry : entryMap.entrySet()) {
//            if (entry.getValue() > maxProbability) {
//                maxProbability = entry.getValue();
//                maxName = entry.getKey();
//            }
//        }
//        return maxName;
        
        // map entries are sorted already (constructor), so we just need to return the first entry
        return CollectionHelper.getFirst(entryMap.keySet());
    }

    @Override
    public boolean contains(String category) {
        return entryMap.containsKey(category);
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("CategoryEntries [");
        boolean first = true;
        for (String categoryName : this) {
            if (first) {
                first = false;
            } else {
                toStringBuilder.append(", ");
            }
            toStringBuilder.append(categoryName);
            toStringBuilder.append("=");
            toStringBuilder.append(MathHelper.round(getProbability(categoryName), 4));
        }
        toStringBuilder.append("]");
        return toStringBuilder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((entryMap == null) ? 0 : entryMap.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImmutableCategoryEntries other = (ImmutableCategoryEntries)obj;
        if (entryMap == null) {
            if (other.entryMap != null)
                return false;
        } else if (!entryMap.equals(other.entryMap))
            return false;
        return true;
    }

}
