package ws.palladian.processing.features;

/**
 * <p>
 * A convenience class for nominal {@code Feature}s binding {@code T} to a {@code String} value.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @author Philipp Katz
 * @version 2.0
 * @since 0.1.7
 */
public class NominalFeature extends Feature<String> {

    /**
     * <p>
     * Creates a new {@code NominalFeature} instance with all attributes initialized.
     * </p>
     * 
     * @param name The {@code FeatureVector} wide unique identifier of this {@code Feature}.
     * @param value The {@code String} value of this {@code Feature}.
     * @param possibleValues The values this {@code NominalFeature} can take on.
     */
    public NominalFeature(final String name, final String value) {
        super(name, value);
    }

}
