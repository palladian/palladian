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
public abstract class Feature<T> {

    /**
     * <p>
     * The {@link FeatureVector} wide unique identifier of this {@code Feature}.
     * </p>
     */
    private final String name;
    /**
     * <p>
     * The {@code Feature}'s value containing concrete extracted data from a document.
     * </p>
     */
    private T value;

    /**
     * <p>
     * Creates a new {@code Feature} with all attributes initialized.
     * </p>
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
     * <p>
     * Creates a new {@link Feature} with all attributes initialized.
     * 
     * @param descriptor The {@link FeatureDescriptor} providing a unique identifier and type information for this
     *            {@link Feature}.
     * @param value The {@link Feature}'s value containing concrete extracted data from a document.
     */
    public Feature(FeatureDescriptor<? extends Feature<T>> descriptor, T value) {
        this(descriptor.getIdentifier(), value);
    }

    /**
     * <p>
     * Provides the {@link FeatureVector} wide unique identifier of this {@code Feature}.
     * </p>
     * 
     * @return The string representing this {@code Feature}s identifier.
     */
    public final String getName() {
        return name;
    }

//    /**
//     * <p>
//     * Resets this {@code Feature}'s identifier overwriting the old one. Use with care!
//     * </p>
//     * 
//     * @param name
//     *            The {@link FeatureVector} wide unique identifier of this {@code Feature}.
//     */
//    public final void setName(String name) {
//        this.name = name;
//    }

    /**
     * <p>
     * Provides the {@code Feature}'s value containing concrete extracted data from a document.
     * </p>
     * 
     * @return The {@code Feature}'s value
     */
    public final T getValue() {
        return value;
    }

    /**
     * <p>
     * Resets and overwrites the {@code Feature}'s value.
     * </p>
     * 
     * @param value
     *            The {@code Feature}'s value containing concrete extracted data
     *            from a document.
     */
    public final void setValue(T value) {
        this.value = value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        // builder.append("Feature [name=");
        // builder.append(name);
        // builder.append(", value=");
        // builder.append(value);
        // builder.append("]");
        builder.append(name);
        builder.append("=");
        builder.append(value);
        return builder.toString();
    }

    /**
     * @return The {@link FeatureDescriptor} for this class.
     */
    public FeatureDescriptor<?> getDescriptor() {
        return FeatureDescriptorBuilder.build(getName(), this.getClass());
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#equals(java.lang.Object)
     */
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
        Feature<?> other = (Feature<?>)obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        if (value == null) {
            if (other.value != null) {
                return false;
            }
        } else if (!value.equals(other.value)) {
            return false;
        }
        return true;
    }

}
