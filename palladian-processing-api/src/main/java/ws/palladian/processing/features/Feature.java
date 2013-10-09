package ws.palladian.processing.features;

/**
 * <p>
 * The base class for all features used by different Information Retrieval and Extraction components inside Palladian. A
 * {@code Feature} can be any information from or about a document that is helpful to guess correct information about
 * that particular document.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @author Philipp Katz
 * @param <T> The data type used to represent this {@code Feature}'s value.
 */
public interface Feature<T> {

    /**
     * <p>
     * Provides the {@link FeatureVector} wide unique identifier of this {@code Feature}.
     * </p>
     * 
     * @return The string representing this {@code Feature}'s identifier.
     */
    String getName();

    /**
     * <p>
     * Provides the {@code Feature}'s value containing concrete extracted data from a document.
     * </p>
     * 
     * @return The {@code Feature}'s value
     */
    T getValue();

}
