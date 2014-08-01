package ws.palladian.helper.functional;

public final class Factories {

    private Factories() {
        // no instances
    }

    /**
     * Create a {@link Factory} which creates the same object every time.
     * 
     * @param thing The object to create.
     * @return A new factory for the given object.
     */
    public static <T> Factory<T> constant(final T thing) {
        return new Factory<T>() {
            @Override
            public T create() {
                return thing;
            }
        };
    }

}
