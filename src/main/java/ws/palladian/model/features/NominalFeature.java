package ws.palladian.model.features;

/**
 * A convenience class for nominal {@code Feature}s binding {@code T} to a {@code String} value.
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 */
public final class NominalFeature extends Feature<String> {

    /**
     * Creates a new {@code NominalFeature} instance with all attributes initialized.
     * 
     * @param name The {@code FeatureVector} wide unique identifier of this {@code Feature}.
     * @param value The {@code String} value of this {@code Feature}.
     */
    public NominalFeature(String name, String value) {
        super(name, value);
    }

}
