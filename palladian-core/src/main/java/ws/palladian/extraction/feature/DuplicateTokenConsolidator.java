package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.Validate;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.TextAnnotationFeature;

/**
 * <p>
 * A {@link PipelineProcessor} which consolidates all duplicate tokens together. The {@link PipelineDocument}s processed
 * by this PipelineProcessor must be tokenized in advance using an Implementation of {@link BaseTokenizer} providing a
 * {@link BaseTokenizer#PROVIDED_FEATURE_DESCRIPTOR}. If a duplicate token (case insensitive) is found, it is removed
 * from the {@link PipelineDocument}'s {@link AnnotationFeature} and put into an {@link AnnotationFeature} to the first
 * occurrence of the token.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class DuplicateTokenConsolidator extends StringDocumentPipelineProcessor {

    private static final long serialVersionUID = 1L;

    public final static FeatureDescriptor<TextAnnotationFeature> DUPLICATES = FeatureDescriptorBuilder.build("duplicates",
            TextAnnotationFeature.class);

    @Override
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        TextAnnotationFeature annotationFeature = document.getFeatureVector()
                .get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("The required feature \""
                    + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR + "\" is missing.");
        }
        SortedMap<String, Annotation<String>> valueMap = new TreeMap<String, Annotation<String>>();
        List<Annotation<String>> resultTokens = new ArrayList<Annotation<String>>();
        for (Annotation<String> currentAnnotation : annotationFeature.getValue()) {
            String tokenValue = currentAnnotation.getValue().toLowerCase();
            if (valueMap.containsKey(tokenValue)) {
                Annotation<String> existingAnnotation = valueMap.get(tokenValue);
                TextAnnotationFeature duplicateAnnotationFeature = existingAnnotation.getFeature(DUPLICATES);
                if (duplicateAnnotationFeature == null) {
                    duplicateAnnotationFeature = new TextAnnotationFeature(DUPLICATES);
                    existingAnnotation.addFeature(duplicateAnnotationFeature);
                }
                duplicateAnnotationFeature.add(currentAnnotation);
            } else {
                valueMap.put(tokenValue, currentAnnotation);
                resultTokens.add(currentAnnotation);
            }
        }
        annotationFeature.setValue(new ArrayList<Annotation<String>>(resultTokens));
    }

    /**
     * <p>
     * Shortcut method to retrieve duplicate {@link Annotation}s which were annotated by this
     * {@link DuplicateTokenConsolidator}.
     * 
     * @param annotation The {@link Annotation} for which to retrieve a {@link List} of duplicate {@link Annotation}s,
     *            not <code>null</code>.
     * @return A list of duplicate {@link Annotation}s, or an empty {@link List} if the {@link Annotation} does not have
     *         any attached duplicate {@link Annotation}s.
     */
    public static List<Annotation<String>> getDuplicateAnnotations(Annotation<String> annotation) {
        Validate.notNull(annotation, "annotation must not be null.");
        TextAnnotationFeature duplicateFeature = annotation.getFeature(DUPLICATES);
        List<Annotation<String>> ret = Collections.emptyList();
        if (duplicateFeature != null) {
            ret = duplicateFeature.getValue();
        }
        return ret;
    }

}
