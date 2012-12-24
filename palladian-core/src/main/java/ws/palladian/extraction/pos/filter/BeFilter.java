/**
 * Created on: 04.12.2012 08:24:15
 */
package ws.palladian.extraction.pos.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Filters all variants of tags for the word to be from the Brown corpus tag set to one "BE" tag.
 * </p>
 * <p>
 * Since this class has no state it is thread safe and reusable.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class BeFilter extends AbstractTagFilter {

    /**
     * <p>
     * Creates a new completely initialized object of this class.
     * </p>
     * 
     * @param postFilter A tag filter processing all tags produced by this filter.
     */
    public BeFilter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * Creates a new completely initialized object of this class.
     * </p>
     */
    public BeFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.toLowerCase().startsWith("be")) {
            ret.add(tag.substring(0, 2));
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
