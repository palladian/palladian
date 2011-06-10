package ws.palladian.model.features;

/**
 * <p>
 * A convenience class for nominal {@code Feature}s binding {@code T} to a {@code String} value.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 */
public final class NominalFeature extends Feature<String> {

    /**
     * <p>
     * Creates a new {@code NominalFeature} instance with all attributes initialized.
     * </p>
     * 
     * @param name The {@code FeatureVector} wide unique identifier of this {@code Feature}.
     * @param value The {@code String} value of this {@code Feature}.
     */
    public NominalFeature(String name, String value) {
        super(name, value);
    }

}
