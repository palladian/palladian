package ws.palladian.extraction.pos;

import java.util.List;

import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.Annotated;

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
    List<Annotated> tag(String text);

}