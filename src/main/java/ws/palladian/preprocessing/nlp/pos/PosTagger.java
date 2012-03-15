package ws.palladian.preprocessing.nlp.pos;

import ws.palladian.preprocessing.nlp.TagAnnotations;

/**
 * <p>
 * Interface defining a POS (Part-of-Speech) Tagger.
 * </p>
 * 
 * @author Philipp Katz
 */
public interface PosTagger {

    /**
     * <p>
     * Get the name of this tagger.
     * </p>
     * 
     * @return the name
     */
    String getName();

    /**
     * <p>
     * Tags a text.
     * </p>
     * 
     * @param sentence The text to tag.
     */
    TagAnnotations tag(String text);

}