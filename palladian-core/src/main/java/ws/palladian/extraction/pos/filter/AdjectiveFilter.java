/**
 * Created on: 05.12.2012 17:42:13
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
public final class AdjectiveFilter extends AbstractTagFilter {

    /**
     * <p>
     * 
     * </p>
     * 
     * @param postFilter
     */
    public AdjectiveFilter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public AdjectiveFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.toLowerCase().startsWith("jj")) {
            ret.add("jj");
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
