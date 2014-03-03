package ws.palladian.classification;

public interface Category {

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
