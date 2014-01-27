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

}
