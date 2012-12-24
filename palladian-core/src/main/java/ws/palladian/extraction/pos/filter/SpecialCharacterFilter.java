/**
 * Created on: 04.12.2012 08:21:06
 */
package ws.palladian.extraction.pos.filter;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A filter that transforms all special characters to one special character tag based on the tags from the Brown corpus.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class SpecialCharacterFilter extends AbstractTagFilter {

    /**
     * <p>
     * Creates a new completely initialized {@link TagFilter}, with the provided filter as post processor. See GoF
     * decorator pattern.
     * </p>
     * 
     * @param postFilter
     */
    public SpecialCharacterFilter(TagFilter postFilter) {
        super(postFilter);
    }

    /**
     * <p>
     * Creates a new completely initialized object of this class.
     * </p>
     */
    public SpecialCharacterFilter() {
        super(null);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.equals("(") || tag.equals(")") || tag.equals("*") || tag.equals(",") || tag.equals("--")
                || tag.equals(".") || tag.equals(":")) {
            ret.add("SPEC");
        } else {
            ret.add(tag);
        }
        return ret;
    }

}
