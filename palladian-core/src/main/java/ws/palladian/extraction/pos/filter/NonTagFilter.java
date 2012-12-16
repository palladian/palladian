/**
 * Created on: 03.12.2012 21:23:49
 */
package ws.palladian.extraction.pos.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A {@link TagFilter} that passes all tags right trough. This is a placeholder in cases where no {@link TagFilter} is
 * required.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class NonTagFilter extends AbstractTagFilter {

    /**
     * <p>
     * Creates a new completely initialized {@link TagFilter}.
     * </p>
     */
    public NonTagFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        ret.add(tag);
        return ret;
    }

}
