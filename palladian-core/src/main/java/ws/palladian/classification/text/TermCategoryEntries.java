package ws.palladian.classification.text;

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
 * the categories internally. The data is stored as a linked list, which might seem odd at first sight, but saves a lot
 * of memory instead of using a HashMap e.g., which has plenty of overhead (imagine, that we keep millions of instances
 * of this class within a dictionary).
 * 
 * @author pk
 * 
 */
public class TermCategoryEntries extends AbstractCategoryEntries {

    private final class CountingCategory implements Category {

        private final String name;
        private int count;
        /** Pointer to the next category (linked list). */
        private CountingCategory next;

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
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            CountingCategory other = (CountingCategory)obj;
            return count == other.count && name.equals(other.name);
        }

    }

    /** An empty, unmodifiable instance of this class (serves as null object). */
    public static final TermCategoryEntries EMPTY = new TermCategoryEntries(null) {
        public void increment(String category) {
            throw new UnsupportedOperationException("This instance is read only and cannot be modified.");
        };
    };

    /**
     * The term, stored as character array to save memory (we have short terms, where String objects have a very high
     * relative overhead).
     */
    private final char[] term;

    /** Pointer to the first category entry (linked list). */
    private CountingCategory first;

    /** The number of category entries. */
    private int categoryCount;

    /** The sum of all counts of all category entries. */
    int totalCount;

    /** Pointer to the next entries; necessary for linking to the next item in the bucket (hash table). */
    TermCategoryEntries next;

    /**
     * Create a new {@link TermCategoryEntries} and set the count for the given category to one.
     * 
     * @param category The category name.
     */
    TermCategoryEntries(String term, String category) {
        Validate.notNull(category, "category must not be null");
        this.term = term.toCharArray();
        this.first = new CountingCategory(category, 1);
        this.categoryCount = 1;
        this.totalCount = 1;
    }

    /**
     * Create a new {@link TermCategoryEntries}. If you need an empty, unmodifiable instance, use {@link #EMPTY}.
     * 
     * @param term The name of the term.
     */
    TermCategoryEntries(String term) {
        this.term = term != null ? term.toCharArray() : new char[0];
        this.first = null;
        this.categoryCount = 0;
        this.totalCount = 0;
    }

    /**
     * Increments a category count by one.
     * 
     * @param category the category to increment, not <code>null</code>.
     */
    void increment(String category) {
        increment(category, 1);
    }

    /**
     * Increments a category count by the given value.
     * 
     * @param category the category to increment, not <code>null</code>.
     * @param count the number by which to increment, greater/equal zero.
     */
    void increment(String category, int count) {
        Validate.notNull(category, "category must not be null");
        Validate.isTrue(count >= 0, "count must be greater/equal zero");
        totalCount += count;
        for (CountingCategory current = first; current != null; current = current.next) {
            if (category.equals(current.getName())) {
                current.count += count;
                return;
            }
        }
        append(category, count);
    }

    private void append(String category, int count) {
        CountingCategory tmp = first;
        first = new CountingCategory(category, count);
        first.next = tmp;
        categoryCount++;
    }

    /**
     * @return The term which is represented by this category entries.
     */
    public String getTerm() {
        return new String(term);
    }

    @Override
    public Iterator<Category> iterator() {
        return new AbstractIterator<Category>() {
            CountingCategory current = first;

            @Override
            protected Category getNext() throws Finished {
                if (current == null) {
                    throw FINISHED;
                }
                Category tmp = current;
                current = current.next;
                return tmp;
            }
        };
    }

    @Override
    public int size() {
        return categoryCount;
    }
    
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append(getTerm()).append(": ");
        boolean first = true;
        for (Category category : this) {
            if (first) {
                first = false;
            } else {
                builder.append(',');
            }
            builder.append(category);
        }
        return builder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        for (Category category : this) {
            result += category.hashCode();
        }
        result = prime * result + Arrays.hashCode(term);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        TermCategoryEntries other = (TermCategoryEntries)obj;
        if (!Arrays.equals(term, other.term)) {
            return false;
        }
        if (this.size() != other.size()) {
            return false;
        }
        for (Category thisCategory : this) {
            int thisCount = thisCategory.getCount();
            int otherCount = other.getCount(thisCategory.getName());
            if (thisCount != otherCount) {
                return false;
            }
        }
        return true;
    }

}
