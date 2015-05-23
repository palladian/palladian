package ws.palladian.classification.text;

import java.util.Iterator;

import ws.palladian.core.AbstractCategoryEntries;
import ws.palladian.core.Category;
import ws.palladian.core.ImmutableCategory;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.math.MathHelper;

final class LinkedCategoryEntries extends AbstractCategoryEntries {
    
    public static final Factory<LinkedCategoryEntries> FACTORY = new Factory<LinkedCategoryEntries>() {
        @Override
        public LinkedCategoryEntries create() {
            return new LinkedCategoryEntries();
        }
    };

    private LinkedCategoryCount firstCategory;

    private int totalCount;

    @Override
    public Iterator<Category> iterator() {
        return new AbstractIterator<Category>() {
            LinkedCategoryCount next = firstCategory;

            @Override
            protected Category getNext() throws Finished {
                if (next == null) {
                    throw FINISHED;
                }
                String categoryName = next.categoryName;
                double probability = (double)next.count / totalCount;
                int count = next.count;
                next = next.nextCategory;
                return new ImmutableCategory(categoryName, probability, count);
            }

        };
    }

    @Override
    public int getTotalCount() {
        return totalCount;
    }

    /**
     * Increments a category count by the given value.
     * 
     * @param category the category to increment, not <code>null</code>.
     * @param count the number by which to increment, greater/equal zero.
     */
    public void increment(String category, int count) {
        for (LinkedCategoryCount current = firstCategory; current != null; current = current.nextCategory) {
            if (category.equals(current.categoryName)) {
                current.count = MathHelper.add(current.count, count);
                totalCount = MathHelper.add(totalCount, count);
                return;
            }
        }
        append(category, count);
    }

    /**
     * Add a category with a given count (no duplicate checking takes place: only to be used, when one can make sure
     * that it does not already exist).
     * 
     * @param category the category to add, not <code>null</code>.
     * @param count the count to set for the category.
     */
    public void append(String category, int count) {
        LinkedCategoryCount tmp = firstCategory;
        firstCategory = new LinkedCategoryCount(category, count);
        firstCategory.nextCategory = tmp;
        totalCount = MathHelper.add(totalCount, count);
    }
    
    public void append(Category category) {
        append(category.getName(), category.getCount());
    }

    private static final class LinkedCategoryCount {
        private final String categoryName;
        private int count;
        private LinkedCategoryCount nextCategory;

        private LinkedCategoryCount(String name, int count) {
            this.categoryName = name;
            this.count = count;
        }
    }


}
