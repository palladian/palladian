/**
 * Created on: 03.12.2012 21:25:56
 */
package ws.palladian.extraction.pos.filter;

import java.util.LinkedList;
import java.util.List;

/**
 * <p>
 * Abstract base class for {@link TagFilter}s.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public abstract class AbstractTagFilter implements TagFilter {
    /**
     * <p>
     * A filter that is run after this filter was applied.
     * </p>
     */
    private TagFilter filter;

    /**
     * <p>
     * Creates a new completely initialized {@link AbstractTagFilter} using another filter as a post processor. This
     * constructor may only be called by subclasses.
     * </p>
     * 
     * @param postFilter A filter that is run after this filter was applied.
     */
    protected AbstractTagFilter(TagFilter postFilter) {
        this.filter = postFilter;
    }

    @Override
    public List<String> filter(String tag) {
        List<String> ret = new LinkedList<String>();
        ret.addAll(internalFilter(tag));
        if (filter != null) {
            List<String> postFilterResult = new LinkedList<String>();
            for (String filteredTag : ret) {
                postFilterResult.addAll(filter.filter(filteredTag));
            }
            return postFilterResult;
        } else {
            return ret;
        }
    }

    /**
     * <p>
     * Internal hook method to implement the filtering algorithm. See GoF Template Method pattern.
     * </p>
     * 
     * @param tag The tag to filter.
     * @return A {@link List} either containing only the filtered version of the provided tag or multiple tags if the
     *         input tag was split by the filter.
     */
    protected abstract List<String> internalFilter(String tag);

}
