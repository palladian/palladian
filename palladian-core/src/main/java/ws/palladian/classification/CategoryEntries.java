package ws.palladian.classification;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * <p>
 * Hold a number of category entries. For example, a word could have a list of relevant categories attached. Each
 * category has a certain relevance for the word which is expressed in the {@link CategoryEntry}.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class CategoryEntries implements Iterable<CategoryEntry> {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(CategoryEntries.class);

    private final List<CategoryEntry> entries = CollectionHelper.newArrayList();

    public CategoryEntry getCategoryEntry(String categoryName) {
        for (CategoryEntry ce : entries) {
            if (ce.getName().equals(categoryName)) {
                return ce;
            }
        }
        return null;
    }

    public void add(CategoryEntry categoryEntry) {
        Validate.notNull(categoryEntry, "categoryEntry must not be null");
        
        CategoryEntry exists = getCategoryEntry(categoryEntry.getName());
        if (exists != null) {
            throw new IllegalStateException();
        }
        
        entries.add(categoryEntry);
        sortByRelevance();
    }

    private void sortByRelevance() {
        Collections.sort(entries, new Comparator<CategoryEntry>() {
            @Override
            public int compare(CategoryEntry o1, CategoryEntry o2) {
                return ((Comparable<Double>)o2.getProbability()).compareTo(o1.getProbability());
            }
        });
    }

    public CategoryEntry getMostLikelyCategoryEntry() {
        sortByRelevance();
        CategoryEntry result = CollectionHelper.getFirst(entries);
        if (result != null) {
            return result;
        }
        LOGGER.warn("no most likey category entry found");
        return new CategoryEntry("", 1);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (CategoryEntry ce : entries) {
            sb.append(ce).append(",");
        }
        return sb.toString();
    }

    @Override
    public Iterator<CategoryEntry> iterator() {
        return entries.iterator();
    }

    public int size() {
        return entries.size();
    }
    
    public static void main(String[] args) {
        CategoryEntries categoryEntries = new CategoryEntries();
        System.out.println(categoryEntries);
        categoryEntries.add(new CategoryEntry("a", 2));
        System.out.println(categoryEntries);
        categoryEntries.add(new CategoryEntry("b", 5));
        System.out.println(categoryEntries);
        categoryEntries.add(new CategoryEntry("c", 1));
        System.out.println(categoryEntries);
        categoryEntries.add(new CategoryEntry("d", 10));
        System.out.println(categoryEntries);
    }

}
