package ws.palladian.classification.text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import ws.palladian.core.AbstractCategoryEntries;
import ws.palladian.core.Category;
import ws.palladian.core.ImmutableCategory;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.math.MathHelper;

/**
 * Store categories in a linked data structure. In contrast to a hash map this
 * saves a considerable amount of memory. Disadvantage: Incrementing a count
 * (during training) is expensive as all entries must be iterated. This is
 * partially mitigated by sorting the entries every now and then by count (thus
 * frequently appearing entries are at the beginning of the list).
 *
 * @author Philipp Katz
 * @deprecated Performance just sucks when training a grid model (i.e. thousands
 *             of categories). I've switched to {@link ArrayCategoryEntries}
 *             which consumes more space, but is better performance-wise.
 */
@Deprecated
final class LinkedCategoryEntries extends AbstractCategoryEntries {
    public static final Factory<LinkedCategoryEntries> FACTORY = LinkedCategoryEntries::new;

    private LinkedCategoryCount firstCategory;

    private int totalCount;

    @Override
    public Iterator<Category> iterator() {
        return new AbstractIterator2<Category>() {
            LinkedCategoryCount next = firstCategory;

            @Override
            protected Category getNext() {
                if (next == null) {
                    return finished();
                }
                String categoryName = next.categoryName;
                double probability = (double) next.count / totalCount;
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
     * @param count    the number by which to increment, greater/equal zero.
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
     * @param count    the count to set for the category.
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

    /** Sort entries in this instance by count. (called every n inserts from the DictionaryTrieModel.Builder). */
    /* package */ void sortByCount() {
        if (firstCategory == null || totalCount < 2) {
            return;
        }
		// can be for sure be done more efficiently, but for now we just put into a
		// list, sort, and then transform it back -- better would be to sort on the
		// linked structure directly, as this avoids instantiating unnecessary objects
        List<LinkedCategoryCount> temp = new ArrayList<>();
        for (LinkedCategoryCount next = firstCategory; next != null; next = next.nextCategory) {
            temp.add(next);
        }
        temp.sort(Comparator.comparing((LinkedCategoryCount entry) -> entry.count).reversed());
        for (int i = 0; i < temp.size() - 1; i++) {
            temp.get(i).nextCategory = temp.get(i + 1);
        }
        temp.get(temp.size() - 1).nextCategory = null;
        firstCategory = temp.get(0);
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
