package ws.palladian.extraction.feature;

import java.util.List;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.ProcessingPipeline;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureVector;

/**
 * <p>
 * A {@link PipelineProcessor} which works on token {@link Annotation}s provided by some implementation of
 * {@link BaseTokenizer}. This means, the {@link ProcessingPipeline} must provide a tokenizer before subclasses of this
 * component do their work, else wise a {@link DocumentUnprocessableException} is thrown. Subclasses of this
 * {@link AbstractTokenProcessor} implement the {@link #processToken(Annotation)} method.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractTokenProcessor extends AbstractPipelineProcessor {

    private static final long serialVersionUID = 1L;

    @Override
    protected final void processDocument(PipelineDocument document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("The required feature \""
                    + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR + " \" is missing.");
        }
        List<Annotation> tokenList = annotationFeature.getValue();
        for (Annotation annotation : tokenList) {
            processToken(annotation);
        }
    }

    /**
     * <p>
     * Process an {@link Annotation} representing a token.
     * </p>
     * 
     * @param annotation The token to process.
     * @throws DocumentUnprocessableException In case of any error, you may throw a
     *             {@link DocumentUnprocessableException}.
     */
    protected abstract void processToken(Annotation annotation) throws DocumentUnprocessableException;

}
