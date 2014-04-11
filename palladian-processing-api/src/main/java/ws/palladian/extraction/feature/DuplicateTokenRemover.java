package ws.palladian.extraction.feature;

import java.util.List;
import java.util.Set;

import ws.palladian.extraction.token.AbstractTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * A {@link PipelineProcessor} which removes all duplicate tokens. The {@link PipelineDocument}s processed by this
 * PipelineProcessor must be tokenized in advance using an Implementation of {@link AbstractTokenizer} providing a
 * {@link AbstractTokenizer#PROVIDED_FEATURE_DESCRIPTOR}.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class DuplicateTokenRemover extends TextDocumentPipelineProcessor {

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        Set<String> tokenValues = CollectionHelper.newHashSet();
        @SuppressWarnings("unchecked")
        List<PositionAnnotation> inputTokens = document.get(ListFeature.class, AbstractTokenizer.PROVIDED_FEATURE);
        ListFeature<PositionAnnotation> resultTokens = new ListFeature<PositionAnnotation>(AbstractTokenizer.PROVIDED_FEATURE);
        for (PositionAnnotation annotation : inputTokens) {
            String tokenValue = annotation.getValue().toLowerCase();
            if (tokenValues.add(tokenValue)) {
                resultTokens.add(annotation);
            }
        }
        document.remove(AbstractTokenizer.PROVIDED_FEATURE);
        document.add(resultTokens);
    }

}
