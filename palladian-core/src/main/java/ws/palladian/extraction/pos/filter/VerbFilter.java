/**
 * Created on: 05.12.2012 18:03:04
 */
package ws.palladian.extraction.pos.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since
 */
public final class VerbFilter extends AbstractTagFilter {

    /**
     * <p>
     * 
     * </p>
     * 
     * @param postFilter
     */
    public VerbFilter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public VerbFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.toLowerCase().startsWith("vb")) {
            ret.add("vb");
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
