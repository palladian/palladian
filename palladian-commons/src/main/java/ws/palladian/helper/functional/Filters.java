package ws.palladian.helper.functional;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;

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
    public static final Filter<Object> NOT_NULL = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return item != null;
        }
    };

    /** A filter which accepts all elements. */
    public static final Filter<Object> ALL = new Filter<Object>() {
        @Override
        public boolean accept(Object item) {
            return true;
        }
    };

    /** A filter which rejects all elements. */
    public static final Filter<Object> NONE = new Filter<Object>() {
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

    public static <T> Filter<T> equal(T value) {
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

    public static <T> Filter<T> chain(Set<Filter<? super T>> filters) {
        Validate.notNull(filters, "filters must not be null");
        return new FilterChain<T>(filters);
    }

    public static <T> Filter<T> chain(Filter<? super T>... filters) {
        Validate.notNull(filters, "filters must not be null");
        return new FilterChain<T>(new HashSet<Filter<? super T>>(Arrays.asList(filters)));
    }

    /**
     * A chain of {@link Filter}s effectively acting as an AND filter, i.e. the processed items need to pass all
     * contained filters, to be accepted by the chain.
     * 
     * @param <T> Type of items to be processed.
     * @author pk
     */
    private static final class FilterChain<T> implements Filter<T> {

        private final Set<Filter<? super T>> filters;

        public FilterChain(Set<Filter<? super T>> filters) {
            this.filters = filters;
        }

        @Override
        public boolean accept(T item) {
            for (Filter<? super T> filter : filters) {
                if (!filter.accept(item)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            builder.append("FilterChain [filters=");
            builder.append(filters);
            builder.append("]");
            return builder.toString();
        }

    }

    /**
     * Get a filter which filters files by their extensions.
     * 
     * @param extensions The extensions to accept (multiple extensions can be given, leading dots are not necessary, but
     *            don't do harm either), not <code>null</code>.
     * @return A filter accepting the given file name extensions.
     */
    public static Filter<File> fileExtension(String... extensions) {
        Validate.notNull(extensions, "extensions must not be null");
        return new FileExtensionFilter(extensions);
    }

    private static final class FileExtensionFilter implements Filter<File> {
        private final Set<String> extensionsSet;

        private FileExtensionFilter(String... extensions) {
            extensionsSet = CollectionHelper.newHashSet();
            for (String extension : extensions) {
                if (extension != null && extension.length() > 0) {
                    if (extension.startsWith(".")) {
                        extension = extension.substring(1);
                    }
                    extensionsSet.add(extension);
                }
            }
        }

        @Override
        public boolean accept(File item) {
            String fileName = item.getName();
            int dotIdx = fileName.lastIndexOf('.');
            if (dotIdx > 0) {
                String extension = fileName.substring(dotIdx + 1);
                return extensionsSet.contains(extension);
            }
            return false;
        }

        @Override
        public String toString() {
            return "FileExtensionFilter " + extensionsSet;
        }

    }

}
