package ws.palladian.core;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.functional.Filter;

/**
 * Filters for {@link Annotation}s and {@link Token}s.
 * 
 * @author pk
 */
public final class AnnotationFilters {

    private AnnotationFilters() {
        // no instances
    }

    public static Filter<Token> range(int start, int end) {
        return new TokenRangeFilter(start, end);
    }

    public static Filter<Annotation> tag(final String tag) {
        Validate.notNull(tag, "tag must not be null");
        return new Filter<Annotation>() {
            @Override
            public boolean accept(Annotation item) {
                return tag.equals(item.getTag());
            }
        };
    }

    private static final class TokenRangeFilter implements Filter<Token> {

        private final int start;
        private final int end;

        private TokenRangeFilter(int start, int end) {
            this.start = start;
            this.end = end;
        }

        @Override
        public boolean accept(Token item) {
            return item.getStartPosition() >= start && item.getEndPosition() <= end;
        }

        @Override
        public String toString() {
            return "TokenRangeFilter [start=" + start + ", end=" + end + "]";
        }

    }

}
