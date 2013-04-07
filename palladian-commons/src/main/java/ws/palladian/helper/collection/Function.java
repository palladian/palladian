package ws.palladian.helper.collection;

/**
 * <p>
 * A function in the classical, mathematical sense; it is a mapping between sets of inputs to a set of outputs.
 * </p>
 * 
 * @author Philipp Katz
 * 
 * @param <I> Type of the input argument.
 * @param <O> Type of the output argument.
 */
public interface Function<I, O> {

    /**
     * <p>
     * A function which maps arbitrary {@link Object}s to their {@link Object#toString()} representation, or
     * <code>null</code> in case the input was <code>null</code>.
     * </p>
     */
    public static final Function<Object, String> TO_STRING_FUNCTION = new Function<Object, String>() {
        @Override
        public String compute(Object input) {
            return input != null ? input.toString() : null;
        }
    };

    // feel free to add further useful default functions here ...

    /**
     * <p>
     * Compute the output value for the given input.
     * </p>
     * 
     * @param input The input, may be <code>null</code>.
     * @return The output, may be <code>null</code>.
     */
    public O compute(I input);

}
