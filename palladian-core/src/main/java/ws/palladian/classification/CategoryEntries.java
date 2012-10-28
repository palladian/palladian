package ws.palladian.classification;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

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

    private final List<CategoryEntry> entries = CollectionHelper.newArrayList();

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
        for (CategoryEntry categoryEntry : entries) {
            if (categoryEntry.getName().equals(categoryName)) {
                return categoryEntry;
            }
        }
        return null;
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
        Iterator<CategoryEntry> iterator = entries.iterator();
        while (iterator.hasNext()) {
            CategoryEntry current = iterator.next();
            if (current.getName().equals(categoryEntry.getName())) {
                iterator.remove();
            }
        }
        entries.add(categoryEntry);
        sortByRelevance();
    }

    /**
     * Sorts the entries by relevance; higher relevance first.
     */
    private void sortByRelevance() {
        Collections.sort(entries, new Comparator<CategoryEntry>() {
            @Override
            public int compare(CategoryEntry e1, CategoryEntry e2) {
                return new Double(e2.getProbability()).compareTo(e1.getProbability());
            }
        });
    }

    /**
     * <p>
     * Retrieves the {@link CategoryEntry} with the highest relevance.
     * </p>
     * 
     * @return The CategoryEntry with the highest relevance, or <code>null</code> no entries exist.
     */
    public CategoryEntry getMostLikelyCategoryEntry() {
        return CollectionHelper.getFirst(entries);
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("CategoryEntries [");
        boolean first = true;
        for (CategoryEntry categoryEntry : entries) {
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
        return entries.iterator();
    }

    /**
     * <p>
     * Retrieve the number of {@link CategoryEntry}s
     * </p>
     * 
     * @return The number of CategoryEntries.
     */
    public int size() {
        return entries.size();
    }

    /**
     * @param entry The entry to search for.
     * @return {@code true} if the {@code CategoryEntries} contain the provided {@code CategoryEntry} and {@code false}
     *         otherwise.
     */
    public boolean contains(CategoryEntry entry) {
        return entries.contains(entry);
    }

}
