package ws.palladian.helper.collection;

/**
 * <p>
 * A specific {@link Factory} for creating constant values.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <T> Type of the instance to create.
 */
public final class ConstantFactory<T> implements Factory<T> {

    private final T thing;
    
    public static <T> ConstantFactory<T> create(T thing) {
        return new ConstantFactory<T>(thing);
    }

    /**
     * @param thing
     */
    private ConstantFactory(T thing) {
        this.thing = thing;
    }

    @Override
    public T create() {
        return thing;
    }

}
