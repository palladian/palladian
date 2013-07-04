package ws.palladian.extraction.feature;

import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.commons.lang.Validate;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

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
public final class DuplicateTokenConsolidator extends TextDocumentPipelineProcessor {

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        List<PositionAnnotation> annotations = document.get(ListFeature.class, BaseTokenizer.PROVIDED_FEATURE);
        SortedMap<String, PositionAnnotation> valueMap = new TreeMap<String, PositionAnnotation>();
        ListFeature<PositionAnnotation> resultTokens = new ListFeature<PositionAnnotation>(BaseTokenizer.PROVIDED_FEATURE);
        for (PositionAnnotation currentAnnotation : annotations) {
            String tokenValue = currentAnnotation.getValue().toLowerCase();
            if (valueMap.containsKey(tokenValue)) {
                PositionAnnotation existingAnnotation = valueMap.get(tokenValue);
                
                existingAnnotation.getFeatureVector().add(currentAnnotation);
            } else {
                valueMap.put(tokenValue, currentAnnotation);
                resultTokens.add(currentAnnotation);
            }
        }
        document.remove(BaseTokenizer.PROVIDED_FEATURE);
        document.add(resultTokens);
    }

    /**
     * <p>
     * Shortcut method to retrieve duplicate {@link PositionAnnotation}s which were annotated by this
     * {@link DuplicateTokenConsolidator}.
     * 
     * @param annotation The {@link PositionAnnotation} for which to retrieve a {@link List} of duplicate {@link PositionAnnotation}s,
     *            not <code>null</code>.
     * @return A list of duplicate {@link PositionAnnotation}s, or an empty {@link List} if the {@link PositionAnnotation} does not have
     *         any attached duplicate {@link PositionAnnotation}s.
     */
    public static List<PositionAnnotation> getDuplicateAnnotations(PositionAnnotation annotation) {
        Validate.notNull(annotation, "annotation must not be null.");
        List<PositionAnnotation> duplicateTokens = annotation.getFeatureVector().get(ListFeature.class, BaseTokenizer.PROVIDED_FEATURE);
//        List<Annotation<String>> ret = Collections.emptyList();
//        if (duplicateFeature != null) {
//            ret = duplicateFeature.getValue();
//        }
//        return ret;
        return duplicateTokens;
    }

}
