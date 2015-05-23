package ws.palladian.helper.functional;

/**
 * <p>
 * A factory is responsible for creating arbitrary objects.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <T> The type of the object to create.
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
