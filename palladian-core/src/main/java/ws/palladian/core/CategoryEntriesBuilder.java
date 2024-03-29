package ws.palladian.core;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.mutable.MutableDouble;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.functional.Factory;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * <p>
 * A builder for producing a {@link CategoryEntries} instance. The probability values of the resulting
 * {@link CategoryEntries} instance are normalized, so that they sum up to one and sorted by probability. The resulting
 * {@link CategoryEntries} object, which can be obtained using {@link #create()}, is immutable. In case, the added
 * scores are negative, the builder assumes that we're dealing with log probabilities; in this case, the probability
 * values are "inverted". NaN/infinity values are not allowed and trigger an {@link IllegalArgumentException}.
 * </p>
 *
 * @author Philipp Katz
 */
public final class CategoryEntriesBuilder implements Factory<CategoryEntries> {
    /** A factory for producing {@link CategoryEntriesBuilder}s. */
    public static final Factory<CategoryEntriesBuilder> FACTORY = CategoryEntriesBuilder::new;

    private final Map<String, MutableDouble> entryMap;

    /**
     * <p>
     * Create a new {@link CategoryEntriesBuilder}.
     * </p>
     */
    public CategoryEntriesBuilder() {
        entryMap = new HashMap<>(1);
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
        entryMap = new HashMap<>();
        for (Entry<String, ? extends Number> entry : map.entrySet()) {
            double score = entry.getValue().doubleValue();
            validateNumber(score);
            entryMap.put(entry.getKey(), new MutableDouble(score));
        }
    }

    /**
     * <p>
     * Set the score of a category name.
     * </p>
     *
     * @param categoryName The name of the category, not <code>null</code>.
     * @param score        The associated score, higher or equal zero.
     * @return Instance of this class, to allow method concatenation.
     */
    public CategoryEntriesBuilder set(String categoryName, double score) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
        validateNumber(score);
        entryMap.put(categoryName, new MutableDouble(score));
        return this;
    }

    /**
     * <p>
     * Set the score of multiple category names.
     * </p>
     *
     * @param categoryNames The names of the categories, not <code>null</code>.
     * @param score         The associated score, higher or equal zero.
     * @return Instance of this class, to allow method concatenation.
     */
    public CategoryEntriesBuilder set(Iterable<String> categoryNames, double score) {
        Validate.notNull(categoryNames, "categoryName must not be null");
        validateNumber(score);
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
     * @param score        The score to add, higher or equal zero.
     * @return Instance of this class, to allow method concatenation.
     */
    public CategoryEntriesBuilder add(String categoryName, double score) {
        Validate.notEmpty(categoryName, "categoryName must not be empty");
        validateNumber(score);
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
            add(category.getName(), category.getProbability());
        }
        return this;
    }

    @Override
    public CategoryEntries create() {
        double total = getTotalScore();
        Map<String, Category> map = new HashMap<>();
        Category mostLikely = null;
        for (Entry<String, MutableDouble> entry : entryMap.entrySet()) {
            double probability;
            if (total == 0) {
                probability = 0.;
            } else {
                probability = entry.getValue().doubleValue() / total;
                if (total < 0) {
                    // in case we have summed up log probabilities; we need the "inverse"
                    probability = 1 - probability;
                }
            }
            String name = entry.getKey();
            if (probability < 0) { // debugging
                throw new IllegalStateException("probability was < 0; this should not happen (obviously caused by mixing negative and positive values)");
            }
            Category category = new ImmutableCategory(name, probability);
            map.put(name, category);
            if (mostLikely == null || mostLikely.getProbability() < probability) {
                mostLikely = category;
            }
        }
        // order by score
        map = CollectionHelper.sortByValue(map, Order.DESCENDING);
        return new ImmutableCategoryEntries(map, mostLikely);
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

    /**
     * Check for infinity/NaN values.
     *
     * @param score The score to check.
     * @throws IllegalArgumentException In case, a NaN/infinity was given.
     */
    private static void validateNumber(double score) {
        if (Double.isNaN(score)) {
            throw new IllegalArgumentException("value was NaN");
        }
        if (Double.isInfinite(score)) {
            throw new IllegalArgumentException("value was infinite");
        }
    }

    @Override
    public String toString() {
        return create().toString();
    }
}
