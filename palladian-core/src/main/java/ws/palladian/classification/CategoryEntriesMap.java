package ws.palladian.classification;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.ConstantFactory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * A {@link CategoryEntries} implementation which uses a {@link Map} internally.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class CategoryEntriesMap implements CategoryEntries {

    private Map<String, Double> entryMap;

    /**
     * <p>
     * Create a new and empty {@link CategoryEntriesMap}. New entries can be added using {@link #set(String, double)}.
     * </p>
     */
    public CategoryEntriesMap() {
        entryMap = CollectionHelper.newHashMap();
    }

    /**
     * <p>
     * Create a new {@link CategoryEntriesMap} by copying an existing one.
     * </p>
     * 
     * @param categoryEntries The {@link CategoryEntries} object to copy, not <code>null</code>.
     */
    public CategoryEntriesMap(CategoryEntries categoryEntries) {
        Validate.notNull(categoryEntries, "categoryEntries must not be null");
        entryMap = CollectionHelper.newHashMap();
        for (String categoryName : categoryEntries) {
            entryMap.put(categoryName, categoryEntries.getProbability(categoryName));
        }
    }

    /**
     * <p>
     * Create a new {@link CategoryEntriesMap} from a given {@link Map} with category and probability values. The values
     * are normalized, so that the sum of all probabilities is one.
     * </p>
     * 
     * @param map The map with categories and probabilities, not <code>null</code>.
     */
    public CategoryEntriesMap(Map<String, Double> map) {
        Validate.notNull(map, "map must not be null");
        entryMap = CollectionHelper.newHashMap();
        double sum = 0;
        for (Double probability : map.values()) {
            sum += probability;
        }
        for (String categoryName : map.keySet()) {
            entryMap.put(categoryName, map.get(categoryName) / sum);
        }
    }

    /**
     * <p>
     * Create a new {@link CategoryEntriesMap} by merging multiple {@link CategoryEntries} instances.
     * </p>
     * 
     * @param categoryEntries The category entries, not <code>null</code>.
     * @return The merged {@link CategoryEntriesMap}.
     */
    public static CategoryEntriesMap merge(CategoryEntries... categoryEntries) {
        Validate.notNull(categoryEntries, "categoryEntries must not be null");
        Map<String, Double> valueMap = LazyMap.create(ConstantFactory.create(0.));
        for (CategoryEntries entries : categoryEntries) {
            for (String category : entries) {
                Double value = valueMap.get(category);
                valueMap.put(category, value + entries.getProbability(category));
            }
        }
        CategoryEntriesMap result = new CategoryEntriesMap();
        for (String category : valueMap.keySet()) {
            result.set(category, valueMap.get(category));
        }
        return result;
    }

    public static CategoryEntriesMap merge(Collection<CategoryEntries> categoryEntries) {
        return merge(categoryEntries.toArray(new CategoryEntries[categoryEntries.size()]));
    }

    @Override
    public double getProbability(String categoryName) {
        Validate.notNull(categoryName, "categoryName must not be null");
        Double result = entryMap.get(categoryName);
        if (result == null) {
            return 0;
        }
        return result;
    }

    /**
     * <p>
     * Set the probability of a category name.
     * </p>
     * 
     * @param categoryName The name of the category, not <code>null</code>.
     * @param probability The associated probability, higher or equal zero.
     */
    public void set(String categoryName, double probability) {
        Validate.notNull(categoryName, "categoryName must not be null");
        Validate.isTrue(probability >= 0, "probability must be higher/equal zero");
        entryMap.put(categoryName, probability);
    }

    /**
     * <p>
     * Add a score to an existing or new category. All other category probabilities will be recalculated.
     * </p>
     * 
     * @param categoryName The name of the category, not <code>null</code>.
     * @param score The score to add, higher or equal zero.
     */
    public void add(String categoryName, double score) {
        Validate.notNull(categoryName, "categoryName must not be null");
        Validate.isTrue(score >= 0, "probability must be higher/equal zero");

        Double existingScore = entryMap.get(categoryName);
        if (existingScore == null) {
            entryMap.put(categoryName, score);
        } else {
            entryMap.put(categoryName, existingScore + score);
        }

    }

    public void addAll(CategoryEntriesMap categories) {
        for (String categoryName : categories) {
            add(categoryName, categories.getProbability(categoryName));
        }
    }

    /**
     * FIXME this overrides the scores and must only be called after all scores were added. Better keep the original
     * scores like in the previous implementation of this class
     * <p>
     * </p>
     */
    public void computeProbabilities() {
        double total = 0;
        for (Entry<String, Double> entry : entryMap.entrySet()) {
            total += entry.getValue();
        }
        Map<String, Double> newEntryMap = new HashMap<String, Double>();
        for (Entry<String, Double> entry : entryMap.entrySet()) {
            if (total == 0) {
                newEntryMap.put(entry.getKey(), 0.);
            } else {
                newEntryMap.put(entry.getKey(), entry.getValue() / total);
            }
        }
        entryMap = newEntryMap;
    }

    @Override
    public String getMostLikelyCategory() {
        double maxProbability = -1;
        String maxName = null;
        for (Entry<String, Double> entry : entryMap.entrySet()) {
            if (entry.getValue() > maxProbability) {
                maxProbability = entry.getValue();
                maxName = entry.getKey();
            }
        }
        return maxName;
    }

    public Entry<String, Double> getMostLikelyCategoryEntry() {
        double maxProbability = -1;
        Entry<String, Double> maxEntry = null;
        for (Entry<String, Double> entry : entryMap.entrySet()) {
            if (entry.getValue() > maxProbability) {
                maxProbability = entry.getValue();
                maxEntry = entry;
            }
        }
        return maxEntry;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("CategoryEntriesMap [");
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
    public Iterator<String> iterator() {
        return entryMap.keySet().iterator();
    }

	@Override
	public boolean contains(String category) {
		return entryMap.keySet().contains(category);
	}

    public boolean isEmpty() {
        return entryMap.isEmpty();
    }

    @Override
    public void sort() {
        entryMap = CollectionHelper.sortByValue(entryMap, false);
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
        CategoryEntriesMap other = (CategoryEntriesMap)obj;
        if (entryMap == null) {
            if (other.entryMap != null)
                return false;
        } else if (!entryMap.equals(other.entryMap))
            return false;
        return true;
    }

}
