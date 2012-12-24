/**
 * Created on: 05.12.2012 17:52:15
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
public final class ModalAuxiliaryFilter extends AbstractTagFilter {

    /**
     * <p>
     * 
     * </p>
     * 
     * @param postFilter
     */
    public ModalAuxiliaryFilter(TagFilter postFilter) {
        super(postFilter);
    }

    public ModalAuxiliaryFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.toLowerCase().startsWith("md")) {
            ret.add("md");
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
