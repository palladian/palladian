package ws.palladian.helper.collection;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

/**
 * Function which serves as adapter, to return a more common type than the given input type. E.g. return
 * <code>Number</code> for given <code>Double</code>. This is useful, when you need to convert an {@link Iterator} to a
 * more common type using the {@link CollectionHelper}.
 * 
 * @author pk
 * 
 * @param <O> Output type.
 * @param <I> Input type.
 * @see CollectionHelper#convert(Iterable, Function)
 * @see CollectionHelper#convert(Iterator, Function)
 */
public final class Adapter<O, I extends O> implements Function<I, O> {

    public static <O, I extends O> Adapter<O, I> create(Class<I> input, Class<O> output) {
        Validate.notNull(input, "input must not be null");
        Validate.notNull(output, "output must not be null");
        return new Adapter<O, I>();
    }

    private Adapter() {
        // created through static method.
    }

    @Override
    public O compute(I input) {
        return input;
    }

}
