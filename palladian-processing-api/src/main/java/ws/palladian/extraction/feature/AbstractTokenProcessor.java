package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.extraction.token.AbstractTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * A {@link PipelineProcessor} which works on token {@link PositionAnnotation}s provided by some implementation of
 * {@link AbstractTokenizer}. This means, the {@link ProcessingPipeline} must provide a tokenizer before subclasses of this
 * component do their work, else wise a {@link DocumentUnprocessableException} is thrown. Subclasses of this
 * {@link AbstractTokenProcessor} implement the {@link #processToken(PositionAnnotation)} method.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractTokenProcessor extends TextDocumentPipelineProcessor {

    @Override
    public final void processDocument(TextDocument document) throws DocumentUnprocessableException {
        List<PositionAnnotation> tokenList = document.get(ListFeature.class, AbstractTokenizer.PROVIDED_FEATURE);
        for (PositionAnnotation annotation : tokenList) {
            processToken(annotation);
        }
    }

    /**
     * <p>
     * Process an {@link PositionAnnotation} representing a token.
     * </p>
     * 
     * @param annotation The token to process.
     * @throws DocumentUnprocessableException In case of any error, you may throw a
     *             {@link DocumentUnprocessableException}.
     */
    protected abstract void processToken(PositionAnnotation annotation) throws DocumentUnprocessableException;

}
