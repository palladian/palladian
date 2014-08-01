package ws.palladian.helper.functional;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

/**
 * Default {@link Filter} implementations.
 * 
 * @author pk
 */
public final class Filters {

    private Filters() {
        // no instances
    }

    /** A filter which removes <code>null</code> elements. */
    public static final Filter<Object> NULL_FILTER = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return item != null;
        }
    };

    /** A filter which accepts all elements. */
    public static final Filter<Object> ACCEPT = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return true;
        }
    };

    /** A filter which rejects all elements. */
    public static final Filter<Object> REJECT = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return false;
        }
    };

    /**
     * Get a filter which inverts a given one. Items which would be accepted by the wrapped Filter are discarded, and
     * vice versa.
     * 
     * @param filter The Filter to wrap, not <code>null</code>.
     * @return A filter with inverted logic of the specified filter.
     */
    public static <T> Filter<T> invert(final Filter<T> filter) {
        Validate.notNull(filter, "filter must not be null");
        return new Filter<T>() {
            @Override
            public boolean accept(T item) {
                return !filter.accept(item);
            }
        };
    }

    public static <T> Filter<T> equals(T value) {
        return new EqualsFilter<T>(Collections.singleton(value));
    }

    public static <T> Filter<T> equal(Collection<T> values) {
        return new EqualsFilter<T>(new HashSet<T>(values));
    }

    public static <T> Filter<T> equal(T... values) {
        return new EqualsFilter<T>(new HashSet<T>(Arrays.asList(values)));
    }

    /**
     * A {@link Filter} which simply filters by Object's equality ({@link Object#equals(Object)}).
     * 
     * @author Philipp Katz
     * @param <T> The type of items to filter.
     */
    private static final class EqualsFilter<T> implements Filter<T> {
        private final Set<T> values;

        private EqualsFilter(Set<T> values) {
            this.values = values;
        }

        @Override
        public boolean accept(T item) {
            return item != null && values.contains(item);
        }
    }

    public static Filter<String> regex(String pattern) {
        Validate.notNull(pattern, "pattern must not be null");
        return new RegexFilter(Pattern.compile(pattern));
    }

    public static Filter<String> regex(Pattern pattern) {
        Validate.notNull(pattern, "pattern must not be null");
        return new RegexFilter(pattern);
    }

    /**
     * A {@link Filter} for {@link String}s using Regex.
     * 
     * @author Philipp Katz
     */
    private static final class RegexFilter implements Filter<String> {

        private final Pattern pattern;

        private RegexFilter(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean accept(String item) {
            if (item == null) {
                return false;
            }
            return pattern.matcher(item).matches();
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("RegexFilter [pattern=");
            builder.append(pattern);
            builder.append("]");
            return builder.toString();
        }

    }

}
