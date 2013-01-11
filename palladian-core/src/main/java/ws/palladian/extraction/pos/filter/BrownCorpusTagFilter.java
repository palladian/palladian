/**
 * Created on: 11.01.2013 23:02:48
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
 * @since 0.2.0
 */
public final class BrownCorpusTagFilter extends AbstractTagFilter {

    /**
     * <p>
     * 
     * </p>
     * 
     * @param postFilter
     */
    protected BrownCorpusTagFilter(TagFilter postFilter) {
        super(postFilter);
    }

    @Override
    protected List<String> internalFilter(String tag) {
        List<String> ret = new ArrayList<String>();
        if (tag.toLowerCase().startsWith("r")) {
            ret.add("r");
        } else if (tag.toLowerCase().startsWith("jj")) {
            ret.add("jj");
        } else if (tag.toLowerCase().startsWith("be")) {
            ret.add(tag.substring(0, 2));
        } else if (tag.toLowerCase().startsWith("ab") || tag.toLowerCase().startsWith("ap")) {
            ret.add("det");
        } else if (tag.toLowerCase().startsWith("fw")) {
            ret.add("fw");
        } else if (tag.toLowerCase().startsWith("hv")) {
            ret.add("hv");
        } else if (tag.toLowerCase().startsWith("md")) {
            ret.add("md");
        } else if (tag.toLowerCase().startsWith("n")) {
            ret.add("n");
        } else if ("cd$".equals(tag) || "cd".equals(tag)) {
            ret.add("num");
        } else if (tag.toLowerCase().startsWith("p")) {
            ret.add("p");
        } else if (tag.equals("(") || tag.equals(")") || tag.equals("*") || tag.equals(",") || tag.equals("--")
                || tag.equals(".") || tag.equals(":")) {
            ret.add("SPEC");
        } else if (tag.toLowerCase().startsWith("vb")) {
            ret.add("vb");
        } else {
            ret.add(tag);
        }
        return ret;
    }
}
