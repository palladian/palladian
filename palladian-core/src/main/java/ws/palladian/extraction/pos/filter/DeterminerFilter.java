/**
 * Created on: 04.12.2012 08:22:56
 */
package ws.palladian.extraction.pos.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * Filters the different forms of determiners from the Brown corpus tag set and maps them to "DET".
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class DeterminerFilter extends AbstractTagFilter {

    /**
     * <p>
     * Creates a new completely initialized object of this class.
     * </p>
     * 
     * @param postFilter
     */
    public DeterminerFilter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * Creates a new completely initialized object of this class.
     * </p>
     */
    public DeterminerFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.toLowerCase().startsWith("ab") || tag.toLowerCase().startsWith("ap")) {
            ret.add("det");
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
