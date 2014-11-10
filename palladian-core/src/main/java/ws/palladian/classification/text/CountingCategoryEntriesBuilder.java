package ws.palladian.classification.text;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

import ws.palladian.core.Category;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.ImmutableCategory;
import ws.palladian.core.ImmutableCategoryEntries;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.math.MathHelper;

public class CountingCategoryEntriesBuilder implements Factory<CategoryEntries> {

    private final Map<String, MutableInt> entryMap;

    public CountingCategoryEntriesBuilder() {
        this.entryMap = CollectionHelper.newHashMap();
    }

    /**
     * <p>
     * Create a new {@link CountingCategoryEntriesBuilder} from a given {@link Map} with category and score values.
     * </p>
     * 
     * @param map The map with categories and scores, not <code>null</code>.
     */
    public CountingCategoryEntriesBuilder(Map<String, ? extends Integer> map) {
        Validate.notNull(map, "map must not be null");
        entryMap = CollectionHelper.newHashMap();
        for (Entry<String, ? extends Number> entry : map.entrySet()) {
            int count = entry.getValue().intValue();
            Validate.isTrue(count >= 0, "count must be greater/equal zero");
            entryMap.put(entry.getKey(), new MutableInt(count));
        }
    }

    public CountingCategoryEntriesBuilder set(String categoryName, int count) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
        Validate.isTrue(count >= 0, "count must be greater/equal zero");
        entryMap.put(categoryName, new MutableInt(count));
        return this;
    }

    public CountingCategoryEntriesBuilder add(String categoryName, int count) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
        Validate.isTrue(count >= 0, "count must be greater/equal zero");
        MutableInt value = entryMap.get(categoryName);
        if (value == null) {
            entryMap.put(categoryName, new MutableInt(count));
        } else {
            value.setValue(MathHelper.add(value.intValue(), count));
        }
        return this;
    }

    public CountingCategoryEntriesBuilder add(CategoryEntries entries) {
        Validate.notNull(entries, "entries must not be null");
        for (Category entry : entries) {
            add(entry.getName(), entry.getCount());
        }
        return this;
    }

//    public CountingCategoryEntriesBuilder subtract(CategoryEntries entries) {
//        Validate.notNull(entries, "entries must not be null");
//        for (Category entry : entries) {
//            subtract(entry.getName(), entry.getCount());
//        }
//        return this;
//    }

    public CountingCategoryEntriesBuilder subtract(String categoryName, int count) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
        Validate.isTrue(count >= 0, "count must be greater/equal zero");
        MutableInt value = entryMap.get(categoryName);
        if (value != null) {
            int newValue = MathHelper.add(value.intValue(), -count);
            value.setValue(newValue);
        }
        return this;
    }

    @Override
    public CategoryEntries create() {
        int totalCount = getTotalCount();
        if (totalCount == 0) {
            return CategoryEntries.EMPTY;
        }
        Map<String, Category> entries = CollectionHelper.newHashMap();
        Category mostLikely = null;
        for (Entry<String, MutableInt> entry : entryMap.entrySet()) {
            int count = entry.getValue().intValue();
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
        int totalCount = 0;
        for (MutableInt value : entryMap.values()) {
            totalCount += value.intValue();
        }
        return totalCount;
    }
    
    public void clear() {
        entryMap.clear();
    }

}
