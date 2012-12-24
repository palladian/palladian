/**
 * Created on: 05.12.2012 18:00:49
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
public final class AdverbFilter extends AbstractTagFilter {

    /**
     * <p>
     * 
     * </p>
     * 
     * @param postFilter
     */
    public AdverbFilter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public AdverbFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.toLowerCase().startsWith("r")) {
            ret.add("r");
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
