package ws.palladian.classification.text;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.AbstractCategoryEntries;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;

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
class CountingCategoryEntries extends AbstractCategoryEntries {

    private final class CountingCategory implements Category {

        private final String name;
        private int count;

        private CountingCategory(String name, int count) {
            this.name = name;
            this.count = count;
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

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + count;
            result = prime * result + name.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null||getClass() != obj.getClass()){
                return false;
            }
            CountingCategory other = (CountingCategory)obj;
            return count == other.count && name.equals(other.name);
        }

    }

    /** An empty, unmodifiable instance of this class (serves as null object). */
    public static final CountingCategoryEntries EMPTY = new CountingCategoryEntries(null) {
        public void increment(String category) {
            throw new UnsupportedOperationException("This instance is read only and cannot be modified.");
        };
    };

    private final char[] term;
    private CountingCategory[] categories;
    private int totalCount;

    CountingCategoryEntries next;

    /**
     * Create a new {@link CountingCategoryEntries} and set the count for the given category to one.
     * 
     * @param category The category name.
     */
    public CountingCategoryEntries(String term, String category) {
        Validate.notNull(category, "category must not be null");
        this.term = term.toCharArray();
        this.categories = new CountingCategory[] {new CountingCategory(category, 1)};
        this.totalCount = 1;
    }

    /**
     * Create a new {@link CountingCategoryEntries}. If you need an empty, unmodifiable instance, use {@link #EMPTY}.
     * 
     * @param term The name of the term.
     */
    public CountingCategoryEntries(String term) {
        this.term = term != null ? term.toCharArray() : new char[0];
        this.categories = new CountingCategory[0];
        this.totalCount = 0;
    }

    /**
     * Increments a category count by one.
     * 
     * @param category the category to increment, not <code>null</code>.
     */
    public void increment(String category) {
        increment(category, 1);
    }

    /**
     * Increments a category count by the given value.
     * 
     * @param category the category to increment, not <code>null</code>.
     * @param count the number by which to increment, greater/equal zero.
     */
    public void increment(String category, int count) {
        Validate.notNull(category, "category must not be null");
        Validate.isTrue(count >= 0, "count must be greater/equal zero");
        totalCount += count;
        for (int i = 0; i < categories.length; i++) {
            if (category.equals(categories[i].getName())) {
                categories[i].count += count;
                return;
            }
        }
        appendToArray(category, count);
    }

    private void appendToArray(String category, int count) {
        CountingCategory[] newCategories = new CountingCategory[categories.length + 1];
        System.arraycopy(categories, 0, newCategories, 0, categories.length);
        newCategories[categories.length] = new CountingCategory(category, count);
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
    public int size() {
        return categories.length;
    }

    @Override
    public int hashCode() {
        return CollectionHelper.newHashSet(categories).hashCode();
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
        return DictionaryModel.equalIgnoreOrder(categories, other.categories);
    }

}
