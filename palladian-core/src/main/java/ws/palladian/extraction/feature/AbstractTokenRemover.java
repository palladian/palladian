package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.TextAnnotationFeature;

/**
 * <p>
 * Base class for token remover implementations. The {@link AbstractTokenRemover} operates on the
 * {@link AnnotationFeature} provided by {@link BaseTokenizer}s. Subclasses implement {@link #remove(Annotation)} to
 * determine, whether to remove an {@link Annotation}.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractTokenRemover extends StringDocumentPipelineProcessor {

    /**
     * <p>
     * Determine whether to remove the supplied {@link Annotation} from the {@link PipelineDocument}'s
     * {@link AnnotationFeature}.
     * </p>
     * 
     * @param annotation The {@link Annotation} for which to determine whether to keep or remove.
     * @return <code>true</code> if {@link Annotation} shall be removed, <code>false</code> otherwise.
     */
    protected abstract boolean remove(Annotation<String> annotation);

    @Override
    public final void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        TextAnnotationFeature annotationFeature = featureVector.get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new DocumentUnprocessableException("Required feature \"" + BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR
                    + "\" is missing");
        }
        List<Annotation<String>> annotations = annotationFeature.getValue();

        // create a new List, as removing many items from an existing one is terribly expensive
        // (unless we were using a LinkedList, what we do not want)
        List<Annotation<String>> resultTokens = new ArrayList<Annotation<String>>();
        for (Iterator<Annotation<String>> tokenIterator = annotations.iterator(); tokenIterator.hasNext();) {
            Annotation<String> annotation = tokenIterator.next();
            if (!remove(annotation)) {
                resultTokens.add(annotation);
            }
        }
        annotationFeature.setValue(resultTokens);
    }

}