package ws.palladian.extraction.feature;

import java.util.Iterator;
import java.util.List;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Base class for token remover implementations. The {@link AbstractTokenRemover} operates on the
 * {@link AnnotationFeature} provided by {@link BaseTokenizer}s. Subclasses implement {@link #remove(PositionAnnotation)} to
 * determine, whether to remove an {@link PositionAnnotation}.
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
        FeatureVector featureVector = document.getFeatureVector();
//        TextAnnotationFeature annotationFeature = featureVector.get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
//        if (annotationFeature == null) {
//            throw new DocumentUnprocessableException("Required feature \"" + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR
//                    + "\" is missing");
//        }
        List<PositionAnnotation> annotations = featureVector.getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);

        // create a new List, as removing many items from an existing one is terribly expensive
        // (unless we were using a LinkedList, what we do not want)
        List<PositionAnnotation> resultTokens = CollectionHelper.newArrayList();
        for (Iterator<PositionAnnotation> tokenIterator = annotations.iterator(); tokenIterator.hasNext();) {
            PositionAnnotation annotation = tokenIterator.next();
            if (!remove(annotation)) {
                resultTokens.add(annotation);
            }
        }
        featureVector.removeAll(BaseTokenizer.PROVIDED_FEATURE);
        featureVector.addAll(resultTokens);
    }

}