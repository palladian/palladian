package ws.palladian.classification;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

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

    private final Map<String, Double> entryMap;

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
    public CategoryEntriesBuilder(Map<String, Double> map) {
        Validate.notNull(map, "map must not be null");
        entryMap = new HashMap<String, Double>(map);
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
        Validate.isTrue(score >= 0, "score must be higher/equal zero");
        entryMap.put(categoryName, score);
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
            entryMap.put(categoryName, score);
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
        Validate.isTrue(score >= 0, "score must be higher/equal zero");
        Double existingScore = entryMap.get(categoryName);
        if (existingScore == null) {
            entryMap.put(categoryName, score);
        } else {
            entryMap.put(categoryName, existingScore + score);
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
        for (String categoryName : categoryEntries) {
            add(categoryName, categoryEntries.getProbability(categoryName));
        }
        return this;
    }

    @Override
    public CategoryEntries create() {
        double total = getTotalScore();
        Map<String, Double> map = CollectionHelper.newHashMap();
        for (Entry<String, Double> entry : entryMap.entrySet()) {
            if (total == 0) {
                map.put(entry.getKey(), 0.);
            } else {
                map.put(entry.getKey(), entry.getValue() / total);
            }
        }
        return new ImmutableCategoryEntries(map);
    }

    /**
     * @return The sum of all scores over all categories.
     */
    private double getTotalScore() {
        double total = 0;
        for (Double value : entryMap.values()) {
            total += value;
        }
        return total;
    }

}
