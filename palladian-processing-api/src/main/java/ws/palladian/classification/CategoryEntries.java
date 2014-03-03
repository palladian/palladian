package ws.palladian.classification;

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

    /**
     * <p>
     * Retrieve the probability of a category.
     * </p>
     * 
     * @param categoryName The category name for which to retrieve the probability, not <code>null</code>.
     * @return The probability, or 0 if no such entry exists.
     */
    double getProbability(String categoryName);

    /**
     * <p>
     * Retrieves the category with the highest relevance.
     * </p>
     * 
     * @return The category with the highest relevance, or <code>null</code> in case no categories were classified.
     * @deprecated Use {@link #getMostLikely()} instead.
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
     * @return <code>true</code> if the category is present, <code>false</code> otherwise.
     */
    boolean contains(String category);

    /**
     * <p>
     * Get a category by name.
     * </p>
     * 
     * @param category The category name.
     * @return The category, or <code>null</code> in case the category is not present.
     */
    Category getCategory(String categoryName);

}
