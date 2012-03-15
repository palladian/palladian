package ws.palladian.preprocessing.nlp.pos;

import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.nlp.TagAnnotations;

/**
 * <p>
 * Interface defining a POS (Part-of-Speech) Tagger.
 * </p>
 * 
 * TODO think about deprecating this in the future and only provide {@link PipelineProcessor}s.
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