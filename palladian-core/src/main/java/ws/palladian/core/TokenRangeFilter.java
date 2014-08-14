package ws.palladian.core;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.functional.Filter;

public final class TokenRangeFilter implements Filter<Token> {

    private final int start;
    private final int end;

    public TokenRangeFilter(int start, int end) {
        Validate.isTrue(start >= 0, "start must be greater/equal zero");
        Validate.isTrue(end >= start, "end must be greater/equal start");
        this.start = start;
        this.end = end;
    }

    @Override
    public boolean accept(Token item) {
        return item.getStartPosition() <= start && item.getEndPosition() <= end;
    }

    @Override
    public String toString() {
        return "TokenRangeFilter [start=" + start + ", end=" + end + "]";
    }

}
