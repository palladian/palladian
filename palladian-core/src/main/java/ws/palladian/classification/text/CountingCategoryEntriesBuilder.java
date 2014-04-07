package ws.palladian.classification.text;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableInt;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.ImmutableCategory;
import ws.palladian.classification.ImmutableCategoryEntries;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;

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
            value.add(count);
        }
        return this;
    }

    @Override
    public CategoryEntries create() {
        int totalCount = getTotalCount();
        if (totalCount == 0) {
            return ImmutableCategoryEntries.EMPTY;
        }
        List<ImmutableCategory> entries = CollectionHelper.newArrayList();
        for (Entry<String, MutableInt> entry : entryMap.entrySet()) {
            int count = entry.getValue().intValue();
            double probability = (double)count / totalCount;
            entries.add(new ImmutableCategory(entry.getKey(), probability, count));
        }
        return new ImmutableCategoryEntries(entries);
    }

    public int getTotalCount() {
        int totalCount = 0;
        for (MutableInt value : entryMap.values()) {
            totalCount += value.intValue();
        }
        return totalCount;
    }

}
