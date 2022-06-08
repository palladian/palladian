package ws.palladian.classification.text;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;

import ws.palladian.core.AbstractCategoryEntries;
import ws.palladian.core.Category;
import ws.palladian.core.ImmutableCategory;
import ws.palladian.helper.math.MathHelper;

/**
 * Category entries stored in an array. Consumes more space than the previously
 * used linked structure, but has at least somewhat O(log n) behavior when
 * incrementing the counts.
 *
 * @author Philipp Katz
 */
class ArrayCategoryEntries extends AbstractCategoryEntries {

    private static final CategoryEntry[] EMPTY = new CategoryEntry[0];

    private static final Comparator<CategoryEntry> ENTRY_COMPARATOR = Comparator.comparing(entry -> entry.name);

    private CategoryEntry[] entries = EMPTY;

    ArrayCategoryEntries() {
        this(EMPTY);
    }

    ArrayCategoryEntries(CategoryEntry[] entries) {
        this.entries = entries;
    }

    @Override
    public Iterator<Category> iterator() {
        int totalCount = Arrays.stream(entries) //
                .mapToInt(entry -> entry.count) //
                .sum();
        return Arrays.stream(entries) //
                .<Category>map(entry -> new ImmutableCategory(entry.name, entry.count, totalCount)) //
                .iterator();
    }

    public void increment(String category, int count) {
        // XXX does intern() give an actual benefit?
        CategoryEntry entry = new CategoryEntry(category.intern(), count);
        int index = Arrays.binarySearch(entries, entry, ENTRY_COMPARATOR);
        if (index >= 0) { // just increment count of existing entry
            entries[index].count = MathHelper.add(entries[index].count, count);
        } else { // add new entry and ensure to keep sorted
            CategoryEntry[] newEntries = new CategoryEntry[entries.length + 1];
            int insertPosition = -index - 1;
            System.arraycopy(entries, 0, newEntries, 0, insertPosition);
            newEntries[insertPosition] = entry;
            System.arraycopy(entries, insertPosition, newEntries, -index, entries.length - insertPosition);
            entries = newEntries;
        }
    }

    static final class CategoryEntry {

        private final String name;

        private int count;

        CategoryEntry(String name, int count) {
            this.name = name;
            this.count = count;
        }

        @Override
        public String toString() {
            return name + "=" + count;
        }

    }

}
