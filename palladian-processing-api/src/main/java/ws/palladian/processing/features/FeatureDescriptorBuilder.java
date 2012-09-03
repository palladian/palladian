package ws.palladian.processing.features;

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

            @Override
            public boolean equals(Object obj) {
                if (this == obj) {
                    return true;
                }
                if (obj == null) {
                    return false;
                }
                if (getClass() != obj.getClass()) {
                    return false;
                }
                FeatureDescriptor<?> other = (FeatureDescriptor<?>)obj;
                if (getIdentifier() == null) {
                    if (other.getIdentifier() != null) {
                        return false;
                    }
                } else if (!getIdentifier().equals(other.getIdentifier())) {
                    return false;
                }
                if (getType() == null) {
                    if (other.getType() != null) {
                        return false;
                    }
                } else if (!getType().equals(other.getType())) {
                    return false;
                }
                return true;
            }

            @Override
            public int hashCode() {
                final int prime = 31;
                int result = 1;
                result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
                result = prime * result + ((type == null) ? 0 : type.hashCode());
                return result;
            }
        };

        return descriptor;
    }

}
