package ws.palladian.classification.text;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.AbstractCategoryEntries;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.helper.collection.AbstractIterator;

/**
 * <p>
 * A mutable {@link CategoryEntries} specifically for use with the text classifier. This class keeps absolute counts of
 * the categories internally. The data is stored in an array, which might seem odd at first sight, but saves a lot of
 * memory instead of using a HashMap e.g., which has plenty of overhead (imagine, that we keep millions of instances of
 * this class within a dictionary).
 * 
 * @author pk
 * 
 */
class CountingCategoryEntries extends AbstractCategoryEntries implements Serializable {

    private static final long serialVersionUID = 1L;

    private final class CountingCategory implements Category, Serializable {
        private static final long serialVersionUID = 1L;

        private final String name;
        private int count;

        private CountingCategory(String name) {
            this.name = name;
            this.count = 1;
        }

        @Override
        public double getProbability() {
            return (double)count / totalCount;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getCount() {
            return count;
        }

        @Override
        public String toString() {
            return name + "=" + count;
        }

    }

    /** An empty, unmodifiable instance of this class (serves as null object). */
    public static final CountingCategoryEntries EMPTY = new CountingCategoryEntries() {
        private static final long serialVersionUID = 1L;

        public void increment(String category) {
            throw new UnsupportedOperationException("This instance is read only and cannot be modified.");
        };
    };

    private final char[] term;
    private CountingCategory[] categories;
    private int totalCount;

    /**
     * Create a new {@link CountingCategoryEntries} and set the count for the given category to one.
     * 
     * @param category The category name.
     */
    public CountingCategoryEntries(String term, String category) {
        Validate.notNull(category, "category must not be null");
        this.term = term.toCharArray();
        this.categories = new CountingCategory[] {new CountingCategory(category)};
        this.totalCount = 1;
    }

    /**
     * Create a new {@link CountingCategoryEntries}. If you need an empty, unmodifiable instance, use {@link #EMPTY}.
     */
    public CountingCategoryEntries() {
        this.term = null;
        this.categories = new CountingCategory[0];
        this.totalCount = 0;
    }

    /**
     * Increments a category count by one.
     * 
     * @param category the category to increment, not <code>null</code>.
     */
    public void increment(String category) {
        Validate.notNull(category, "category must not be null");
        totalCount++;
        for (int i = 0; i < categories.length; i++) {
            if (category.equals(categories[i].getName())) {
                categories[i].count++;
                return;
            }
        }
        appendToArray(category);
    }

    private void appendToArray(String category) {
        CountingCategory[] newCategories = new CountingCategory[categories.length + 1];
        System.arraycopy(categories, 0, newCategories, 0, categories.length);
        newCategories[categories.length] = new CountingCategory(category);
        categories = newCategories;
    }
    
    public String getTerm() {
        return new String(term);
    }

    @Override
    public Iterator<Category> iterator() {
        return new AbstractIterator<Category>() {
            int idx = 0;

            @Override
            protected Category getNext() throws Finished {
                if (idx >= categories.length) {
                    throw FINISHED;
                }
                return categories[idx++];
            }
        };
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(categories);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        CountingCategoryEntries other = (CountingCategoryEntries)obj;
        return Arrays.equals(categories, other.categories);
    }

}
