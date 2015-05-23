package ws.palladian.helper.functional;

import java.util.Map;

import org.apache.commons.lang3.Validate;

public final class Functions {

    private Functions() {
        // no instances
    }

    /**
     * <p>
     * A function which maps arbitrary {@link Object}s to their {@link Object#toString()} representation, or
     * <code>null</code> in case the input was <code>null</code>.
     */
    public static final Function<Object, String> TO_STRING = new Function<Object, String>() {
        @Override
        public String compute(Object input) {
            return input != null ? input.toString() : null;
        }
    };

    /**
     * <p>
     * A function which converts {@link String}s to lowercase, or <code>null</code> in case the input was
     * <code>null</code>.
     */
    public static final Function<String, String> LOWERCASE = new Function<String, String>() {
        @Override
        public String compute(String input) {
            return input != null ? input.toLowerCase() : null;
        }
    };

    public static <I, O> Function<I, O> map(Map<? extends I, ? extends O> map) {
        Validate.notNull(map, "map must not be null");
        return new MappingFunction<I, O>(map);
    }

    /**
     * {@link Function} which maps values as determined by a {@link Map}.
     * 
     * @author pk
     * 
     * @param <I> Input type.
     * @param <O> Output type.
     */
    private static final class MappingFunction<I, O> implements Function<I, O> {

        private final Map<? extends I, ? extends O> map;

        public MappingFunction(Map<? extends I, ? extends O> map) {
            this.map = map;
        }

        @Override
        public O compute(I input) {
            return map.get(input);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("MappingFunction [map=");
            builder.append(map);
            builder.append("]");
            return builder.toString();
        }

    }

    /**
     * <p>
     * Creates a {@link Function} which serves as adapter, to return a more common type than the given input type. E.g.
     * return <code>Number</code> for given <code>Double</code>. This is useful, when you need to convert an
     * {@link Iterator} to a more common type using the {@link CollectionHelper}.
     * 
     * @param input Type of the input, not <code>null</code>.
     * @param output Type of the output, must be superclass of input, not <code>null</code>.
     * @return The function.
     */
    public static <O, I extends O> Function<I, O> adapt(Class<I> input, Class<O> output) {
        Validate.notNull(input, "input must not be null");
        Validate.notNull(output, "output must not be null");
        return new Function<I, O>() {
            @Override
            public I compute(I input) {
                return input;
            }
        };
    }

}
