package ws.palladian.core;

/**
 * <p>
 * The {@link CategoryEntries} represent a result from a {@link Classifier}. It typically holds a number of potential
 * categories, each associated with a probability which can be retrieved using {@link #getProbability(String)}. The
 * interface also includes {@link Iterable}, which allows to loop over all assigned categories. The iteration order is
 * arbitrary, i.e. it is not necessarily ordered by probabilities.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface CategoryEntries extends Iterable<Category> {

    /** An empty instance. */
    public static final CategoryEntries EMPTY = new ImmutableCategoryEntries();

    /**
     * <p>
     * Retrieve the probability of a category.
     * </p>
     * 
     * @param categoryName The category name for which to retrieve the probability, not <code>null</code>.
     * @return The probability, or <code>0</code> if no such entry exists.
     */
    double getProbability(String categoryName);

    /**
     * <p>
     * Retrieve the count of a category.
     * </p>
     * 
     * @param categoryName The category name for which to retrieve the count, not <code>null</code>.
     * @return The count, or <code>0</code> if no such entry exists, or <code>-1</code> if the category provides no
     *         count.
     */
    int getCount(String categoryName);

    /**
     * <p>
     * Retrieves the category with the highest relevance.
     * </p>
     * 
     * @return The category with the highest relevance, or <code>null</code> in case no categories were classified.
     */
    String getMostLikelyCategory();

    /**
     * <p>
     * Retrieves the category with the highest relevance.
     * </p>
     * 
     * @return The category with the highest relevance, or <code>null</code> in case when no categories were classified.
     */
    Category getMostLikely();

    /**
     * <p>
     * Check whether a category is present.
     * </p>
     * 
     * @param category The category name, not <code>null</code>.
     * @return <code>true</code> if the category is present, <code>false</code> otherwise.
     */
    boolean contains(String category);

    /**
     * <p>
     * Get a category by name.
     * </p>
     * 
     * @param category The category name, not <code>null</code>.
     * @return The category, or <code>null</code> in case the category is not present.
     */
    Category getCategory(String categoryName);

    /**
     * @return The number of (non-null) categories.
     */
    int size();

    /**
     * @return The sum of all category counts, or <code>-1</code>, in case no counts are available.
     */
    int getTotalCount();

}
