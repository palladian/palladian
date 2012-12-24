/**
 * Created on: 04.12.2012 22:15:34
 */
package ws.palladian.extraction.pos.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Filters all forms of the verb "HV" based on the Brown corpus tag set to the PoS tag "HV".
 * </p>
 * <p>
 * Since objects of this class have no state it is thread safe and reusable.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class HaveFilter extends AbstractTagFilter {

    /**
     * <p>
     * Creates a new completely initialized object of this class.
     * </p>
     * 
     * @param postFilter A filter that is run on the result of this filter.
     */
    public HaveFilter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * Creates a new completely initialized object of this class.
     * </p>
     */
    public HaveFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.toLowerCase().startsWith("hv")) {
            ret.add("hv");
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
