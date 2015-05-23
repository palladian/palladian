package ws.palladian.helper.functional;

import org.apache.commons.lang3.Validate;

/**
 * <p>
 * This class allows sequentially chaining of multiple functions. It provides syntactic sugar by using the builder
 * pattern (start by invoking static {@link #with(Function)} method), which ensures type safety, and does not allow
 * chaining of incompatible functions, and finally produces a typed {@link Function} representing the actual chain.
 * Example usage:
 * 
 * <pre>
 * Function&lt;String, Long&gt; function = LinkedFunctionBuilder // use the #with method
 *         .with(new StringToIntegerFunction()) // transforming String to Integer
 *         .add(new NumberSquareRootFunction()) // transforming Number to Double
 *         .add(new RoundFunction()) // transforming Number to Long
 *         .create(); // result is transforming String to Long
 * 
 * // notice the type safety, we know that the input is String,
 * // and the output is Long
 * Long result = function.compute(&quot;two&quot;);
 * </pre>
 * 
 * @author pk
 */
public final class LinkedFunctionBuilder {

    /**
     * <p>
     * Create a new linked function with the given {@link Function} as first function.
     * 
     * @param function The first function in the chain, not <code>null</code>.
     * @return A builder, which allows adding more functions.
     */
    public static <I, O> Builder<I, O> with(Function<I, O> function) {
        Validate.notNull(function, "function must not be null");
        return new Builder<I, O>(function);
    }

    public static class Builder<I, E> {

        private final Function<I, E> firstFunction;

        /** Instantiation only from enclosing class. */
        private Builder(Function<I, E> function) {
            this.firstFunction = function;
        }

        /**
         * <p>
         * Connect the given function with the existing predecessor.
         * 
         * @param function The function to connect, not <code>null</code>.
         * @return A builder, which allows adding more function.
         */
        public <O> Builder<I, O> add(Function<? super E, O> function) {
            Validate.notNull(function, "function must not be null");
            return new Builder<I, O>(new LinkedFunction<I, E, O>(firstFunction, function));
        }

        /**
         * <p>
         * Create the final {@link Function}.
         * 
         * @return The function which consists of all specified functions.
         */
        public Function<I, E> create() {
            return firstFunction;
        }

    }

    /**
     * <p>
     * A function connecting two functions.
     * 
     * @author pk
     * 
     * @param <I> Input type into first function.
     * @param <E> Exchange type between first and second function.
     * @param <O> Output type of second function.
     */
    private static final class LinkedFunction<I, E, O> implements Function<I, O> {
        private final Function<I, E> f1;
        private final Function<? super E, O> f2;

        public LinkedFunction(Function<I, E> f1, Function<? super E, O> f2) {
            this.f1 = f1;
            this.f2 = f2;
        }

        @Override
        public O compute(I input) {
            return f2.compute(f1.compute(input));
        }

        @Override
        public String toString() {
            return f1.toString() + " > " + f2.toString();
        }

    }

}
