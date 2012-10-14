package ws.palladian.helper.collection;

public final class ConstantFactory<T> implements Factory<T> {

    private final T thing;

    /**
     * @param thing
     */
    public ConstantFactory(T thing) {
        this.thing = thing;
    }

    @Override
    public T create() {
        return thing;
    }

}
