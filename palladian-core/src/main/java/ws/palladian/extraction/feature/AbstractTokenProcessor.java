package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * A {@link PipelineProcessor} which works on token {@link PositionAnnotation}s provided by some implementation of
 * {@link BaseTokenizer}. This means, the {@link ProcessingPipeline} must provide a tokenizer before subclasses of this
 * component do their work, else wise a {@link DocumentUnprocessableException} is thrown. Subclasses of this
 * {@link AbstractTokenProcessor} implement the {@link #processToken(PositionAnnotation)} method.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractTokenProcessor extends StringDocumentPipelineProcessor {

    @Override
    public final void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
//        if (annotationFeature == null) {
//            throw new DocumentUnprocessableException("The required feature \""
//                    + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR + " \" is missing.");
//        }
        List<PositionAnnotation> tokenList = featureVector.getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);
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
