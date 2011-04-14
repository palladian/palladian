package ws.palladian.model.features;

/**
 * A convenience class for numeric {@code Feature}s binding {@code T} to a {@code Double} value.
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 */
public final class NumericFeature extends Feature<Double> {

    /**
     * Creates a new {@code NumericFeature} instance with all attributes initialized.
     * 
     * @param name The {@link FeatureVector} wide unique identifier of this {@code Feature}.
     * @param value The numeric value of this {@code Feature}.
     */
    public NumericFeature(String name, Double value) {
        super(name, value);
    }

}
