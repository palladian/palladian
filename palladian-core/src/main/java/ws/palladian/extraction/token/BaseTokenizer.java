package ws.palladian.extraction.token;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.Validate;

import ws.palladian.extraction.feature.StringDocumentPipelineProcessor;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.TextAnnotationFeature;

/**
 * <p>
 * Abstract base class for tokenizer annotators.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class BaseTokenizer extends StringDocumentPipelineProcessor {

    /**
     * <p>
     * The identifier of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final String PROVIDED_FEATURE = "ws.palladian.features.tokens";

    /**
     * <p>
     * The descriptor of the feature provided by this {@link PipelineProcessor}.
     * </p>
     */
    public static final FeatureDescriptor<TextAnnotationFeature> PROVIDED_FEATURE_DESCRIPTOR = FeatureDescriptorBuilder
            .build(PROVIDED_FEATURE, TextAnnotationFeature.class);

    /**
     * <p>
     * Shortcut method to retrieve token {@link Annotation}s which were supplied by one of the {@link BaseTokenizer}
     * implementations.
     * </p>
     * 
     * @param document The document for which to retrieve the token annotations, not <code>null</code>.
     * @return List of token annotations.
     * @throws IllegalStateException In case the document does not provide any token annotations.
     */
    public static List<Annotation<String>> getTokenAnnotations(PipelineDocument<String> document) {
        Validate.notNull(document, "document must not be null");
        FeatureVector featureVector = document.getFeatureVector();
        TextAnnotationFeature annotationFeature = featureVector.get(PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new IllegalStateException(
                    "The document does not provide token annotations, process it with a Tokenizer annotator first.");
        }
        return annotationFeature.getValue();
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
    public static List<String> getTokens(PipelineDocument<String> document) {
        Validate.notNull(document, "document must not be null");
        List<String> tokens = new ArrayList<String>();
        List<Annotation<String>> tokenAnnotations = getTokenAnnotations(document);
        for (Annotation<String> annotation : tokenAnnotations) {
            tokens.add(annotation.getValue());
        }
        return tokens;
    }
}
