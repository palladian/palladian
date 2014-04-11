package ws.palladian.extraction.feature;

import java.util.Iterator;
import java.util.List;

import ws.palladian.extraction.token.AbstractTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Base class for token remover implementations. The {@link AbstractTokenRemover} operates on the
 * {@link AnnotationFeature} provided by {@link AbstractTokenizer}s. Subclasses implement
 * {@link #remove(PositionAnnotation)} to determine, whether to remove a {@link PositionAnnotation}.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractTokenRemover extends TextDocumentPipelineProcessor {

    /**
     * <p>
     * Determine whether to remove the supplied {@link PositionAnnotation} from the {@link PipelineDocument}'s
     * {@link AnnotationFeature}.
     * </p>
     * 
     * @param annotation The {@link PositionAnnotation} for which to determine whether to keep or remove.
     * @return <code>true</code> if {@link PositionAnnotation} shall be removed, <code>false</code> otherwise.
     */
    protected abstract boolean remove(PositionAnnotation annotation);

    @Override
    public final void processDocument(TextDocument document) throws DocumentUnprocessableException {
        @SuppressWarnings("unchecked")
        ListFeature<PositionAnnotation> annotations = document.get(ListFeature.class, AbstractTokenizer.PROVIDED_FEATURE);

        // create a new List, as removing many items from an existing one is terribly expensive
        // (unless we were using a LinkedList, what we do not want)
        ListFeature<PositionAnnotation> resultTokens = new ListFeature<PositionAnnotation>(AbstractTokenizer.PROVIDED_FEATURE);
        for (Iterator<PositionAnnotation> tokenIterator = annotations.iterator(); tokenIterator.hasNext();) {
            PositionAnnotation annotation = tokenIterator.next();
            if (!remove(annotation)) {
                resultTokens.add(annotation);
            }
        }
        document.remove(AbstractTokenizer.PROVIDED_FEATURE);
        document.add(resultTokens);
    }

}