package ws.palladian.helper.functional;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.CollectionHelper;

/**
 * Default {@link Predicate} implementations.
 * 
 * @author Philipp Katz
 */
public final class Filters {

    private Filters() {
        // no instances
    }

    /** A filter which removes <code>null</code> elements. */
    public static final Predicate<Object> NOT_NULL = new Predicate<Object>() {
        @Override
        public boolean test(Object item) {
            return item != null;
        }
        @Override
        public String toString() {
        	return "!= null";
        };
    };

    /** A filter which accepts all elements. */
    public static final Predicate<Object> ALL = new Predicate<Object>() {
        @Override
        public boolean test(Object item) {
            return true;
        }
        @Override
        public String toString() {
        	return "true";
        };
    };

    /** A filter which rejects all elements. */
    public static final Predicate<Object> NONE = new Predicate<Object>() {
        @Override
        public boolean test(Object item) {
            return false;
        }
        @Override
        public String toString() {
        	return "false";
        };
    };

    /** A filter which rejects empty {@link CharSequence}s. */
    public static final Predicate<CharSequence> EMPTY = new Predicate<CharSequence>() {
        @Override
        public boolean test(CharSequence item) {
            return item != null && item.length() > 0;
        }
        @Override
        public String toString() {
        	return "length() > 0";
        };
    };

    /**
     * Get a filter which inverts a given one. Items which would be accepted by the wrapped Filter are discarded, and
     * vice versa.
     * 
     * @param filter The Filter to wrap, not <code>null</code>.
     * @return A filter with inverted logic of the specified filter.
     */
    public static <T> Predicate<T> not(final Predicate<T> filter) {
        Validate.notNull(filter, "filter must not be null");
        return new Predicate<T>() {
            @Override
            public boolean test(T item) {
                return !filter.test(item);
            }
            @Override
            public String toString() {
            	return "! " + filter;
            }
        };
    }

    public static <T> Predicate<T> equal(T value) {
        return new EqualsFilter<>(Collections.singleton(value));
    }

    public static <T> Predicate<T> equal(Collection<T> values) {
        return new EqualsFilter<>(new HashSet<T>(values));
    }

    @SafeVarargs
    public static <T> Predicate<T> equal(T... values) {
        return new EqualsFilter<>(new HashSet<T>(Arrays.asList(values)));
    }

    /**
     * A {@link Predicate} which simply filters by Object's equality ({@link Object#equals(Object)}).
     * 
     * @author Philipp Katz
     * @param <T> The type of items to filter.
     */
    private static final class EqualsFilter<T> implements Predicate<T> {
        private final Set<T> values;

        private EqualsFilter(Set<T> values) {
            this.values = values;
        }

        @Override
        public boolean test(T item) {
            return item != null && values.contains(item);
        }
        
        @Override
        public String toString() {
        	return values.size() == 1 ? values.iterator().next().toString() : values.toString();
        }
    }

    public static Predicate<String> regex(String pattern) {
        Validate.notNull(pattern, "pattern must not be null");
        return new RegexFilter(Pattern.compile(pattern));
    }

    public static Predicate<String> regex(Pattern pattern) {
        Validate.notNull(pattern, "pattern must not be null");
        return new RegexFilter(pattern);
    }
    
	public static Predicate<String> contains(final String substring) {
		Validate.notNull(substring, "substring must not be null");
		return new Predicate<String>() {
			@Override
			public boolean test(String item) {
				return item.contains(substring);
			}
		};
	}

    /**
     * A {@link Predicate} for {@link String}s using Regex.
     * 
     * @author Philipp Katz
     */
    private static final class RegexFilter implements Predicate<String> {

        private final Pattern pattern;

        private RegexFilter(Pattern pattern) {
            this.pattern = pattern;
        }

        @Override
        public boolean test(String item) {
            if (item == null) {
                return false;
            }
            return pattern.matcher(item).matches();
        }

        @Override
        public String toString() {
        	return pattern.toString();
        }

    }

    /**
     * <p>
     * Combine multiple filters so that they act as <code>AND</code> combination (i.e. each of the given filters needs
     * to accept an item). The filters are processed in the given order.
     * 
     * @param filters The filters to combine, not <code>null</code>.
     * @return An <code>AND</code>-combination of the given filters.
     */
    public static <T> Predicate<T> and(Collection<? extends Predicate<? super T>> filters) {
        Validate.notNull(filters, "filters must not be null");
        return new AndFilter<>(filters);
    }

    /**
     * <p>
     * Combine multiple filters so that they act as <code>AND</code> combination (i.e. each of the given filters needs
     * to accept an item). The filters are processed in the given order.
     * 
     * @param filters The filters to combine, not <code>null</code>.
     * @return An <code>AND</code>-combination of the given filters.
     */
    @SafeVarargs
    public static <T> Predicate<T> and(Predicate<? super T>... filters) {
        Validate.notNull(filters, "filters must not be null");
        return new AndFilter<>(new LinkedHashSet<>(Arrays.asList(filters)));
    }
    
	/**
	 * Combine multiple filters so that they act as <code>OR</code> combination
	 * (i.e. at least one of the given filters needs to accept and item). The
	 * filters are processed in the given order using short-circuit evaluation.
	 * 
	 * @param filters
	 *            The filters to combine, not <code>null</code>.
	 * @return An <code>OR</code>-combination of the given filters.
	 */
	public static <T> Predicate<T> or(Collection<? extends Predicate<? super T>> filters) {
		Validate.notNull(filters, "filters must not be null");
		return new OrFilter<>(filters);
	}
    
	/**
	 * Combine multiple filters so that they act as <code>OR</code> combination
	 * (i.e. at least one of the given filters needs to accept and item). The
	 * filters are processed in the given order using short-circuit evaluation.
	 * 
	 * @param filters
	 *            The filters to combine, not <code>null</code>.
	 * @return An <code>OR</code>-combination of the given filters.
	 */
    @SafeVarargs
	public static <T> Predicate<T> or(Predicate<? super T>... filters) {
    	Validate.notNull(filters, "filters must not be null");
    	return new OrFilter<>(new LinkedHashSet<>(Arrays.asList(filters)));
    }

    /**
     * A chain of {@link Predicate}s effectively acting as an AND filter, i.e. the processed items need to pass all
     * contained filters, to be accepted by the chain.
     * 
     * @param <T> Type of items to be processed.
     * @author Philipp Katz
     */
    private static final class AndFilter<T> implements Predicate<T> {

        private final Collection<? extends Predicate<? super T>> filters;

        AndFilter(Collection<? extends Predicate<? super T>> filters) {
            this.filters = filters;
        }

        @Override
        public boolean test(T item) {
            for (Predicate<? super T> filter : filters) {
                if (!filter.test(item)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
        	return "(" + StringUtils.join(filters, " && ") + ")";
        }

    }
    
    private static final class OrFilter<T> implements Predicate<T> {
    	private final Collection<? extends Predicate<? super T>> filters;

		OrFilter(Collection<? extends Predicate<? super T>> filters) {
			this.filters = filters;
		}

		@Override
		public boolean test(T item) {
			for (Predicate<? super T> filter : filters) {
				if (filter.test(item)) {
					return true;
				}
			}
			return false;
		}
		
		@Override
		public String toString() {
			return "(" + StringUtils.join(filters, " || ") + ")";
		}
    	
    }

    /**
     * Get a filter which filters files (i.e. no directories) by their extensions.
     * 
     * @param extensions The extensions to accept (multiple extensions can be given, leading dots are not necessary, but
     *            don't do harm either), not <code>null</code>.
     * @return A filter accepting the given file name extensions.
     */
    public static Predicate<File> fileExtension(String... extensions) {
        Validate.notNull(extensions, "extensions must not be null");
        return new FileExtensionFilter(extensions);
    }

    private static final class FileExtensionFilter implements Predicate<File> {
        private final Set<String> extensionsSet;

        private FileExtensionFilter(String... extensions) {
            extensionsSet = new HashSet<>();
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
        public boolean test(File item) {
            if (item.isFile()) {
                String fileName = item.getName();
                int dotIdx = fileName.lastIndexOf('.');
                if (dotIdx > 0) {
                    String extension = fileName.substring(dotIdx + 1);
                    return extensionsSet.contains(extension);
                }
            }
            return false;
        }

        @Override
        public String toString() {
            return "FileExtensionFilter " + extensionsSet;
        }

    }

    /**
     * Get a filter which accepts directories.
     * 
     * @return A filter accepting directories.
     */
    public static Predicate<File> directory() {
        return new Predicate<File>() {
            @Override
            public boolean test(File item) {
                return item.isDirectory();
            }
        };
    }
    
	/**
	 * Get a filter which accepts files.
	 * 
	 * @return A filter accepting files.
	 */
	public static Predicate<File> file() {
		return new Predicate<File>() {
			@Override
			public boolean test(File item) {
				return item.isFile();
			}
		};
	}

    /**
     * Get a filter by file names.
     * 
     * @param names The names to accept, not <code>null</code>.
     * @return A filter accepting files with the specified names.
     */
    public static Predicate<File> fileName(String... names) {
        Validate.notNull(names, "names must not be null");
        return new FileNameFilter(names);
    }

    private static final class FileNameFilter implements Predicate<File> {
        private final Set<String> nameSet;

        public FileNameFilter(String... names) {
            nameSet = CollectionHelper.newHashSet(names);
        }

        @Override
        public boolean test(File item) {
            return nameSet.contains(item.getName());
        }
        
        @Override
        public String toString() {
            return "FileNameFilter " + nameSet;
        }

    }

}
