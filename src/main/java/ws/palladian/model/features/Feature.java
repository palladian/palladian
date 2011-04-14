package ws.palladian.model.features;

/**
 * The base class for all features used by different Information Retrieval and
 * Extraction components inside palladian. A {@code Feature} can be any
 * information from or about a document that is helpful to guess correct
 * information about that particular document.
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @see ws.palladian.classification.Classifier
 * @param <T>
 *            The data type used to represent this {@code Feature}s value.
 */
public class Feature<T> {
    /**
     * The {@link FeatureVector} wide unique identifier of this {@code Feature}.
     */
    private String name;
    /**
     * The {@code Feature}'s value containing concrete extracted data from a
     * document.
     */
    private T value;

    /**
     * Creates a new {@code Feature} with all attributes initialized.
     * 
     * @param name
     *            The {@link FeatureVector} wide unique identifier of this {@code Feature}.
     * @param value
     *            The {@code Feature}'s value containing concrete extracted data
     *            from a document.
     */
    public Feature(String name, T value) {
        super();
        this.name = name;
        this.value = value;
    }

    /**
     * Provides the {@link FeatureVector} wide unique identifier of this {@code Feature}.
     * 
     * @return The string representing this {@code Feature}s identifier.
     */
    public final String getName() {
        return name;
    }

    /**
     * Resets this {@code Feature}'s identifier overwriting the old one. Use
     * with care!
     * 
     * @param name
     *            The {@link FeatureVector} wide unique identifier of this {@code Feature}.
     */
    public final void setName(String name) {
        this.name = name;
    }

    /**
     * Provides the {@code Feature}'s value containing concrete extracted data
     * from a document.
     * 
     * @return The {@code Feature}'s value
     */
    public final T getValue() {
        return value;
    }

    /**
     * Resets and overwrites the {@code Feature}'s value.
     * 
     * @param value
     *            The {@code Feature}'s value containing concrete extracted data
     *            from a document.
     */
    public final void setValue(T value) {
        this.value = value;
    }
}
