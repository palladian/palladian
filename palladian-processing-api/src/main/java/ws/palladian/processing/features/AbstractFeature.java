/**
 * Created on: 24.06.2013 11:10:08
 */
package ws.palladian.processing.features;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * Abstract base class for all dense features.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.2
 */
abstract class AbstractFeature<T> implements Feature<T> {

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
    private final T value;

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
    protected AbstractFeature(String name, T value) {
        Validate.notEmpty(name);
        Validate.notNull(value);

        this.name = name;
        this.value = value;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final T getValue() {
        return value;
    }

    @Override
    public String toString() {
        return String.format("%s=%s", name, value);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
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
        AbstractFeature<?> other = (AbstractFeature<?>)obj;
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
