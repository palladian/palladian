/**
 * Created on: 05.12.2012 17:54:49
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
public final class NounFilter extends AbstractTagFilter {

    /**
     * <p>
     * 
     * </p>
     * 
     * @param postFilter
     */
    public NounFilter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public NounFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.toLowerCase().startsWith("n")) {
            ret.add("n");
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
