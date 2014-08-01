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

}
