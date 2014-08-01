package ws.palladian.helper.functional;

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
     * Compute the output value for the given input.
     * </p>
     * 
     * @param input The input, may be <code>null</code>.
     * @return The output, may be <code>null</code>.
     */
    public O compute(I input);

}
