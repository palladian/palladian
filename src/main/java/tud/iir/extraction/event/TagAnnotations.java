package tud.iir.extraction.event;

import java.util.ArrayList;

public class TagAnnotations extends ArrayList<TagAnnotation> {

    /**
     * 
     */
    private static final long serialVersionUID = -328843608844181172L;

    public ArrayList<String> getTagList() {

        final ArrayList<String> tagList = new ArrayList<String>();

        for (final TagAnnotation tag : this) {
            tagList.add(tag.getTag());
        }

        return tagList;
    }

    public ArrayList<String> getTokenList() {

        final ArrayList<String> tokenList = new ArrayList<String>();

        for (final TagAnnotation tag : this) {
            tokenList.add(tag.getChunk());
        }

        return tokenList;
    }

    public String getTaggedString() {

        final StringBuffer out = new StringBuffer();

        for (final TagAnnotation tag : this) {
            out.append(tag.getChunk());
            out.append("/");
            out.append(tag.getTag());
            out.append(" ");
        }

        return out.toString();

    }
}
