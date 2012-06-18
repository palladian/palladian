package ws.palladian.model.features;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Helper for creating {@link FeatureDescriptor}s.
 * </p>
 * 
 * @author Philipp Katz
 * 
 */
public final class FeatureDescriptorBuilder {

    private FeatureDescriptorBuilder() {
        // helper class, prevent instantiation.
    }

    /**
     * <p>
     * Create a new {@link FeatureDescriptor} for the specified identifier and type.
     * </p>
     * 
     * @param identifier The unique identifier of this {@link FeatureDescriptor}, e.g.
     *            <code>ws.palladian.features.tokens</code>. Not <code>null</code>, not empty.
     * @param type The run-time type of this feature, not <code>null</code>.
     * @return
     */
    public static <T extends Feature<?>> FeatureDescriptor<T> build(final String identifier, final Class<T> type) {
        Validate.notEmpty(identifier, "Identifier must be supplied");
        Validate.notNull(type, "Type must be supplied.");

        FeatureDescriptor<T> descriptor = new FeatureDescriptor<T>() {
            @Override
            public String getIdentifier() {
                return identifier;
            }

            @Override
            public Class<T> getType() {
                return type;
            }

            @Override
            public String toString() {
                return identifier + " (" + type.getSimpleName() + ")";
            }
        };

        return descriptor;
    }

}
