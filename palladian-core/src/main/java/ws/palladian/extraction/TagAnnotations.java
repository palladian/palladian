package ws.palladian.extraction;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.extraction.pos.PosTagger;

import com.aliasi.util.Arrays;

/**
 * <p>
 * A list of PoS tag annotations on a text. This class allows fast access to the attributes of each annotation.
 * </p>
 * 
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 * @see PosTagger
 */
@Deprecated
public class TagAnnotations extends ArrayList<TagAnnotation> {

    private static final long serialVersionUID = -328843608844181172L;

    /**
     * <p>
     * Get the tagged string with PoS annotations.
     * </p>
     * 
     * @return the tagged string
     */
    public String getTaggedString() {

        final StringBuffer out = new StringBuffer();

        for (final TagAnnotation tag : this) {
            out.append(tag.getChunk());
            out.append('/');
            out.append(tag.getTag());
            out.append(' ');
        }

        return out.toString();

    }

    /**
     * <p>
     * Provides the list of tags saved in this list.
     * </p>
     * 
     * @return The part of speech tags saved in this list.
     */
    public List<String> getTagList() {

        final ArrayList<String> tagList = new ArrayList<String>();

        for (final TagAnnotation tag : this) {
            tagList.add(tag.getTag());
        }

        return tagList;
    }

    /**
     * <p>
     * Provides all entries fitting one of the provided tags. This method is like a filter or white list for tags.
     * </p>
     * 
     * @param tags The tags to consider in the returned tag list.
     */
    public List<TagAnnotation> getTagList(String[] tags) {
        ArrayList<TagAnnotation> ret = new ArrayList<TagAnnotation>();
        for (TagAnnotation tag : this) {
            if (Arrays.member(tag.getTag(), tags)) {
                ret.add(tag);
            }
        }
        return ret;
    }

    /**
     * <p>
     * Provides the list of tokens, which are the actual words PoS tags refer to.
     * </p>
     * 
     * @return The tokens for all tags saved in this list.
     */
    public List<String> getTokenList() {

        ArrayList<String> tokenList = new ArrayList<String>(this.size());

        for (TagAnnotation tag : this) {
            tokenList.add(tag.getChunk());
        }

        return tokenList;
    }

    public boolean containTag(String tag) {
        return getTagList().contains(tag);
    }
}
