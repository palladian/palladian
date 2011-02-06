package tud.iir.preprocessing.nlp;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * A list of PoS tag annotations on a text. This class allows fast access to the attributes of each annotation.
 * </p>
 * 
 * @author Martin Wunderwald
 * @author Klemens Muthmann
 * @see AbstractPOSTagger
 */
public class TagAnnotations extends ArrayList<TagAnnotation> {

    private static final long serialVersionUID = -328843608844181172L;

    /**
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
     * @return The part of speach tags saved in this list.
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
     * Provides the list of token, which are the actual words PoS tags refer to.
     * </p>
     * 
     * @return The tokens for all tags saved in this list.
     */
    public List<String> getTokenList() {

        final ArrayList<String> tokenList = new ArrayList<String>();

        for (final TagAnnotation tag : this) {
            tokenList.add(tag.getChunk());
        }

        return tokenList;
    }
}
