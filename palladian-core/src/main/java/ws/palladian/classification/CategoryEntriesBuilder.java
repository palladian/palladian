package ws.palladian.classification;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableDouble;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Factory;

/**
 * <p>
 * A builder for producing a {@link CategoryEntries} instance. The probability values of the resulting
 * {@link CategoryEntries} instance are normalized, so that they sum up to one and sorted by probability. The resulting
 * {@link CategoryEntries} object, which can be obtained using {@link #create()}, is immutable.
 * </p>
 * 
 * @author pk
 * 
 */
public final class CategoryEntriesBuilder implements Factory<CategoryEntries> {

    private final Map<String, MutableDouble> entryMap;

    /**
     * <p>
     * Create a new {@link CategoryEntriesBuilder}.
     * </p>
     */
    public CategoryEntriesBuilder() {
        entryMap = CollectionHelper.newHashMap();
    }

    /**
     * <p>
     * Create a new {@link CategoryEntriesBuilder} from a given {@link Map} with category and score values.
     * </p>
     * 
     * @param map The map with categories and scores, not <code>null</code>.
     */
    public CategoryEntriesBuilder(Map<String, ? extends Number> map) {
        Validate.notNull(map, "map must not be null");
        entryMap = CollectionHelper.newHashMap();
        for (Entry<String, ? extends Number> entry : map.entrySet()) {
            entryMap.put(entry.getKey(), new MutableDouble(entry.getValue()));
        }
    }

    /**
     * <p>
     * Set the score of a category name.
     * </p>
     * 
     * @param categoryName The name of the category, not <code>null</code>.
     * @param score The associated score, higher or equal zero.
     * @return Instance of this class, to allow method concatenation.
     */
    public CategoryEntriesBuilder set(String categoryName, double score) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
//        Validate.isTrue(score >= 0, "score must be higher/equal zero");
        MutableDouble value = entryMap.get(categoryName);
        if (value == null) {
            entryMap.put(categoryName, new MutableDouble(score));
        } else {
            value.setValue(score);
        }
        return this;
    }

    /**
     * <p>
     * Set the score of multiple category names.
     * </p>
     * 
     * @param categoryNames The names of the categories, not <code>null</code>.
     * @param score The associated score, higher or equal zero.
     * @return Instance of this class, to allow method concatenation.
     */
    public CategoryEntriesBuilder set(Iterable<String> categoryNames, double score) {
        Validate.notNull(categoryNames, "categoryName must not be null");
        Validate.isTrue(score >= 0, "score must be higher/equal zero");
        for (String categoryName : categoryNames) {
            set(categoryName, score);
        }
        return this;
    }

    /**
     * <p>
     * Add a score to an existing or new category.
     * </p>
     * 
     * @param categoryName The name of the category, not <code>null</code>.
     * @param score The score to add, higher or equal zero.
     * @return Instance of this class, to allow method concatenation.
     */
    public CategoryEntriesBuilder add(String categoryName, double score) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
//        Validate.isTrue(score >= 0, "score must be higher/equal zero");
        MutableDouble value = entryMap.get(categoryName);
        if (value == null) {
            entryMap.put(categoryName, new MutableDouble(score));
        } else {
            value.add(score);
        }
        return this;
    }

    /**
     * <p>
     * Add another {@link CategoryEntries} instance to this builder. Scores are summed.
     * </p>
     * 
     * @param categoryEntries The {@link CategoryEntries} to add, not <code>null</code>.
     * @return Instance of this class, to allow method concatenation.
     */
    public CategoryEntriesBuilder add(CategoryEntries categoryEntries) {
        Validate.notNull(categoryEntries, "categoryEntries must not be null");
        for (Category category : categoryEntries) {
//            add(category.getName(), category.getProbability());
            if (category.getCount() != -1) {
                add(category.getName(), category.getCount());
            } else {
                add(category.getName(), category.getProbability());
            }
        }
        return this;
    }

    @Override
    public CategoryEntries create() {
        double total = getTotalScore();
        Map<String, Double> map = CollectionHelper.newHashMap();
        for (Entry<String, MutableDouble> entry : entryMap.entrySet()) {
            if (total == 0) {
                map.put(entry.getKey(), 0.);
            } else {
                map.put(entry.getKey(), entry.getValue().doubleValue() / total);
            }
        }
        return new ImmutableCategoryEntries(map);
    }

    /**
     * @return The sum of all scores over all categories.
     */
    public double getTotalScore() {
        double total = 0;
        for (MutableDouble value : entryMap.values()) {
            total += value.doubleValue();
        }
        return total;
    }

    /**
     * <p>
     * Get the score of a given category.
     * </p>
     * 
     * @param categoryName The category name, not <code>null</code>.
     * @return The score, or zero in case no score was set for the categoy.
     */
    public double getScore(String categoryName) {
        MutableDouble value = entryMap.get(categoryName);
        return value != null ? value.doubleValue() : 0;
    }

}
