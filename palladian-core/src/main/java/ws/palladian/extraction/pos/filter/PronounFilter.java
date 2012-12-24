/**
 * Created on: 05.12.2012 17:58:15
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
public final class PronounFilter extends AbstractTagFilter {

    /**
     * <p>
     * 
     * </p>
     * 
     * @param postFilter
     */
    public PronounFilter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public PronounFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.toLowerCase().startsWith("p")) {
            ret.add("p");
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
