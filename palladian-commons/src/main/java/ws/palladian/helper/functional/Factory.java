package ws.palladian.helper.functional;

/**
 * <p>
 * A factory is responsible for creating arbitrary objects.
 * </p>
 *
 * @param <T> The type of the object to create.
 * @author Philipp Katz
 * @see Factories for default implementations.
 */
public interface Factory<T> {

    /**
     * <p>
     * Create a new object.
     * </p>
     *
     * @return The object.
     */
    T create();
}
