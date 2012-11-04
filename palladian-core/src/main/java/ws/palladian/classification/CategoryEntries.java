package ws.palladian.classification;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.MathHelper;

/**
 * <p>
 * Hold a number of categories. For example, a word could have a list of relevant categories attached. Each category has
 * a certain relevance which is expressed in the {@link CategoryEntry}.
 * </p>
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class CategoryEntries implements Iterable<CategoryEntry> {

    private Map<String, Double> categoryEntries = CollectionHelper.newHashMap();

    /**
     * <p>
     * Retrieve a {@link CategoryEntry} by its name.
     * </p>
     * 
     * @param categoryName The name of the CategoryEntry to retrieve, not <code>null</code>.
     * @return The CategoryEntry with the specified name, or <code>null</code> if no such entry exists.
     */
    public CategoryEntry getCategoryEntry(String categoryName) {
        Validate.notNull(categoryName, "categoryName must not be null");
        Double probability = categoryEntries.get(categoryName);
        if (probability == null) {
            return null;
        }
        return new CategoryEntry(categoryName, probability);
    }

    /**
     * <p>
     * Adds a new {@link CategoryEntry} to the list. If a CategoryEntry with the specified name already exists, it is
     * replaced by the supplied one.
     * </p>
     * 
     * @param categoryEntry The CategoryEntry to add, not <code>null</code>.
     */
    public void add(CategoryEntry categoryEntry) {
        Validate.notNull(categoryEntry, "categoryEntry must not be null");
        categoryEntries.put(categoryEntry.getName(), categoryEntry.getProbability());
        sort();
    }

    private void sort() {
        categoryEntries = CollectionHelper.sortByValue(categoryEntries, false);
    }

    /**
     * <p>
     * Adds multiple {@link CategoryEntry} objects to the list.
     * </p>
     * 
     * @param categoryEntries
     */
    public void addAll(CategoryEntries categoryEntries) {
        for (CategoryEntry categoryEntry : categoryEntries) {
            add(categoryEntry);
        }
    }

    /**
     * <p>
     * Adds multiple {@link CategoryEntry} objects to the list.
     * </p>
     * 
     * @param categoryEntries
     */
    public void addAll(Collection<CategoryEntry> categoryEntries) {
        for (CategoryEntry categoryEntry : categoryEntries) {
            add(categoryEntry);
        }
    }

    /**
     * <p>
     * Retrieves the {@link CategoryEntry} with the highest relevance.
     * </p>
     * 
     * @return The CategoryEntry with the highest relevance, or <code>null</code> no entries exist.
     */
    public CategoryEntry getMostLikelyCategoryEntry() {
        if (categoryEntries.isEmpty()) {
            return null;
        }
        double maxProbability = -1;
        String maxName = null;
        for (Entry<String, Double> entry : categoryEntries.entrySet()) {
            if (entry.getValue() > maxProbability) {
                maxProbability = entry.getValue();
                maxName = entry.getKey();
            }
        }
        return new CategoryEntry(maxName, maxProbability);
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("CategoryEntries [");
        boolean first = true;
        for (CategoryEntry categoryEntry : this) {
            if (first) {
                first = false;
            } else {
                toStringBuilder.append(", ");
            }
            toStringBuilder.append(categoryEntry.getName());
            toStringBuilder.append("=");
            toStringBuilder.append(MathHelper.round(categoryEntry.getProbability(), 4));
        }
        toStringBuilder.append("]");
        return toStringBuilder.toString();
    }

    @Override
    public Iterator<CategoryEntry> iterator() {
        final Iterator<Entry<String, Double>> iterator = categoryEntries.entrySet().iterator();
        return new Iterator<CategoryEntry>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public CategoryEntry next() {
                Entry<String, Double> entry = iterator.next();
                return new CategoryEntry(entry.getKey(), entry.getValue());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Modifications are not allowed.");
            }
        };
    }

    /**
     * <p>
     * Retrieve the number of {@link CategoryEntry}s
     * </p>
     * 
     * @return The number of CategoryEntries.
     */
    public int size() {
        return categoryEntries.size();
    }

//    /**
//     * @param entry The entry to search for.
//     * @return {@code true} if the {@code CategoryEntries} contain the provided {@code CategoryEntry} and {@code false}
//     *         otherwise.
//     */
//    public boolean contains(CategoryEntry entry) {
//        return categoryEntries.containsKey(entry);
//    }

}
