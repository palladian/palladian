package ws.palladian.classification.text;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.ImmutableCategory;
import ws.palladian.core.ImmutableCategoryEntries;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.math.MathHelper;

public class CountingCategoryEntriesBuilder implements Factory<CategoryEntries> {

    private final Object2IntMap<String> entryMap;

    public CountingCategoryEntriesBuilder() {
        this.entryMap = new Object2IntOpenHashMap<String>();
    }

    /**
     * <p>
     * Create a new {@link CountingCategoryEntriesBuilder} from a given {@link Map} with category and score values.
     * </p>
     * 
     * @param map The map with categories and scores, not <code>null</code>.
     */
    public CountingCategoryEntriesBuilder(Map<String, ? extends Integer> map) {
        this();
        Validate.notNull(map, "map must not be null");
        for (Entry<String, ? extends Number> entry : map.entrySet()) {
            int count = entry.getValue().intValue();
            Validate.isTrue(count >= 0, "count must be greater/equal zero");
            entryMap.put(entry.getKey(), count);
        }
    }

    public CountingCategoryEntriesBuilder set(String categoryName, int count) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
        Validate.isTrue(count >= 0, "count must be greater/equal zero");
        entryMap.put(categoryName, count);
        return this;
    }

    public CountingCategoryEntriesBuilder add(String categoryName, int count) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
        Validate.isTrue(count >= 0, "count must be greater/equal zero, but was " + count);
        entryMap.compute(categoryName, (key, value) -> value != null ? MathHelper.add(value, count) : count);
        return this;
    }

    public CountingCategoryEntriesBuilder add(CategoryEntries entries) {
        Validate.notNull(entries, "entries must not be null");
        for (Category entry : entries) {
            add(entry.getName(), entry.getCount());
        }
        return this;
    }

    public CountingCategoryEntriesBuilder subtract(String categoryName, int count) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
        Validate.isTrue(count >= 0, "count must be greater/equal zero");
        entryMap.compute(categoryName, (key, value) -> value != null ? MathHelper.add(value, -count) : null);
        return this;
    }

    @Override
    public CategoryEntries create() {
        int totalCount = getTotalCount();
        if (totalCount == 0) {
            return CategoryEntries.EMPTY;
        }
        Map<String, Category> entries = new Object2ObjectOpenHashMap<>();
        Category mostLikely = null;
        for (Object2IntMap.Entry<String> entry : entryMap.object2IntEntrySet()) {
            int count = entry.getIntValue();
            if (count == 0) { // skip zero entries
                continue;
            }
            double probability = (double)count / totalCount;
            String name = entry.getKey();
            ImmutableCategory category = new ImmutableCategory(name, probability, count);
            entries.put(name, category);
            if (mostLikely == null || mostLikely.getProbability() < probability) {
                mostLikely = category;
            }
        }
        return new ImmutableCategoryEntries(entries, mostLikely);
    }

    public int getTotalCount() {
    	return entryMap.values().intStream().sum();
    }
    
    public void clear() {
        entryMap.clear();
    }

}
