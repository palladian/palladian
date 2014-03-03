package ws.palladian.classification.text;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Iterator;

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
final class CountingCategoryEntries extends AbstractCategoryEntries implements Serializable {

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

    /** An empty instance of this class (serves as null object). */
    public static final CountingCategoryEntries EMPTY = new CountingCategoryEntries();

    private CountingCategory[] categories;
    private int totalCount;

    public CountingCategoryEntries(String category) {
        this.categories = new CountingCategory[] {new CountingCategory(category)};
        this.totalCount = 1;
    }

    private CountingCategoryEntries() {
        this.categories = new CountingCategory[0];
        this.totalCount = 0;
    }

    /**
     * Increments a category count by one.
     * 
     * @param category the category to increment, not <code>null</code>.
     */
    public void increment(String category) {
        totalCount++;
        for (int i = 0; i < categories.length; i++) {
            if (category.equals(categories[i].name)) {
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

    @Override
    public Iterator<Category> iterator() {
        return new AbstractIterator<Category>() {
            int idx = 0;

            @Override
            protected Category getNext() throws Finished {
                if (idx >= categories.length) {
                    throw FINISHED;
                }
                CountingCategory category = categories[idx];
                idx++;
                return category;
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
