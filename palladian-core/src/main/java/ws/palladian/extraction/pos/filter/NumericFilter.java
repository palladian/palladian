/**
 * Created on: 04.12.2012 08:24:55
 */
package ws.palladian.extraction.pos.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Filters all tags from the Brown corpus tag set and replaces them with "NUM".
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class NumericFilter extends AbstractTagFilter {

    /**
     * <p>
     * Creates a new completely initialized object of this class.
     * </p>
     * 
     * @param postFilter A filter filtering the result of this filter.
     */
    public NumericFilter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * Creates a new completely initialized object of this class.
     * </p>
     */
    public NumericFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if ("cd$".equals(tag) || "cd".equals(tag)) {
            ret.add("num");
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
