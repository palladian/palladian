/**
 * Created on: 04.12.2012 08:31:29
 */
package ws.palladian.extraction.pos.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Filters all types of foreign word PoS tags in the form of the Brown tag set to the "fw" tag.
 * </p>
 * <p>
 * Since objects of this class have no internal state they should be thread safe and reusable.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class ForeignWordFilter extends AbstractTagFilter {

    /**
     * <p>
     * Creates a new completely initialized instance of this class.
     * </p>
     * 
     * @param postFilter Another tag filter run on the results from this tag filter.
     */
    public ForeignWordFilter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * Creates a new completely initialized instance of this class.
     * </p>
     */
    public ForeignWordFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.toLowerCase().startsWith("fw")) {
            ret.add("fw");
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
