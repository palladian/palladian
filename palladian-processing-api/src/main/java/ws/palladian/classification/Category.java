package ws.palladian.classification;

/**
 * A category with name and probability, and an optional absolute count. Comparison should be implemented in such a way,
 * that Categories are sorted by probability in descending order (ie. high probability categories first).
 * 
 * @author pk
 * 
 */
public interface Category extends Comparable<Category> {

    /**
     * @return The probability of this category in the range [0,1]
     */
    double getProbability();

    /**
     * @return The name of this category.
     */
    String getName();

    /**
     * @return An absolute count of this category, or <code>-1</code> in case this value is not provided.
     */
    int getCount();

}
