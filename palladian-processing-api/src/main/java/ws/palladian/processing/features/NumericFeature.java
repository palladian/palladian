package ws.palladian.processing.features;

/**
 * <p>
 * A convenience class for numeric {@code Feature}s binding {@code T} to a {@code Double} value.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @author Philipp Katz
 */
public class NumericFeature extends AbstractFeature<Double> {

    /**
     * <p>
     * Creates a new {@code NumericFeature} instance with all attributes initialized.
     * </p>
     * 
     * @param name The {@link BasicFeatureVectorImpl} wide unique identifier of this {@code Feature}.
     * @param value The numeric value of this {@code Feature}.
     */
    public NumericFeature(String name, Number value) {
        super(name, value != null ? value.doubleValue() : null);
    }

}
