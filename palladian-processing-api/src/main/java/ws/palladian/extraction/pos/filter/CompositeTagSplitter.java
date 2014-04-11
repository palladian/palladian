/**
 * Created on: 04.12.2012 09:23:03
 */
package ws.palladian.extraction.pos.filter;

import java.util.Arrays;
import java.util.List;

/**
 * <p>
 * Splits tags connected by "+" based on the Brown corpus tag set.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class CompositeTagSplitter extends AbstractTagFilter {

    /**
     * <p>
     * Creates a new completely initlaized object of this class.
     * </p>
     * 
     * @param postFilter A post processer filtering all results of this processor.
     */
    public CompositeTagSplitter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * Creates a new completely initialized object of this class.
     * </p>
     */
    public CompositeTagSplitter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        return Arrays.asList(tag.split("\\+"));
    }

}
