package ws.palladian.core;

import org.apache.commons.lang3.Validate;

import java.util.function.Predicate;

/**
 * Filters for {@link Annotation}s and {@link Token}s.
 *
 * @author Philipp Katz
 */
public final class AnnotationFilters {

    private AnnotationFilters() {
        // no instances
    }

    public static Predicate<Token> range(int start, int end) {
        return new TokenRangeFilter(start, end);
    }

    public static Predicate<Annotation> tag(final String tag) {
        Validate.notNull(tag, "tag must not be null");
        return new Predicate<Annotation>() {
            @Override
            public boolean test(Annotation item) {
                return tag.equals(item.getTag());
            }
        };
    }

    private static final class TokenRangeFilter implements Predicate<Token> {

        private final int start;
        private final int end;

        private TokenRangeFilter(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean test(Token item) {
            return item.getStartPosition() >= start && item.getEndPosition() <= end;
        }

        @Override
        public String toString() {
            return "TokenRangeFilter [start=" + start + ", end=" + end + "]";
        }

    }

}
