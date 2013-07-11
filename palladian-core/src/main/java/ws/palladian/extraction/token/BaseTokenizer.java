package ws.palladian.extraction.token;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import ws.palladian.extraction.feature.TextDocumentPipelineProcessor;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Abstract base class for tokenizer annotators.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class BaseTokenizer extends TextDocumentPipelineProcessor {

    /**
     * <p>
     * The identifier of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final String PROVIDED_FEATURE = "ws.palladian.features.tokens";

    /**
     * <p>
     * Shortcut method to retrieve {@link PositionAnnotation}s which were supplied by one of the {@link BaseTokenizer}
     * implementations.
     * </p>
     * 
     * @param document The document for which to retrieve the token annotations, not <code>null</code>.
     * @return List of token annotations.
     * @throws IllegalStateException In case the document does not provide any token annotations.
     */
    @SuppressWarnings("unchecked")
    public static ListFeature<PositionAnnotation> getTokenAnnotations(TextDocument document) {
        Validate.notNull(document, "document must not be null");
        return document.get(ListFeature.class, PROVIDED_FEATURE);
    }

    /**
     * <p>
     * Shortcut method to retrieve token values which were supplied by one of the {@link BaseTokenizer} implementations.
     * </p>
     * 
     * @param document The document for which to retrieve the token values, not <code>null</code>.
     * @return List of token values.
     * @throws IllegalStateException In case the document does not provide any token annotations.
     */
    public static List<String> getTokens(TextDocument document) {
        Validate.notNull(document, "document must not be null");
        List<String> tokens = new ArrayList<String>();
        List<PositionAnnotation> annotations = getTokenAnnotations(document);
        for (PositionAnnotation annotation : annotations) {
            tokens.add(annotation.getValue());
        }
        return tokens;
    }
}
