package ws.palladian.extraction.feature;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.FeatureVector;

/**
 * <p>
 * Base class for token remover implementations. The {@link AbstractTokenRemover} operates on the
 * {@link AnnotationFeature} provided by {@link TokenizerInterface}s. Subclasses implement {@link #remove(Annotation)}
 * to determine, whether to remove an {@link Annotation}.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractTokenRemover implements PipelineProcessor {

    private static final long serialVersionUID = 1L;

    /**
     * <p>
     * Determine whether to remove the supplied {@link Annotation} from the {@link PipelineDocument}'s
     * {@link AnnotationFeature}.
     * </p>
     * 
     * @param annotation The {@link Annotation} for which to determine whether to keep or remove.
     * @return <code>true</code> if {@link Annotation} shall be removed, <code>false</code> otherwise.
     */
    protected abstract boolean remove(Annotation annotation);

    @Override
    public final void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR);
        if (annotationFeature == null) {
            throw new IllegalStateException("Required feature \"" + TokenizerInterface.PROVIDED_FEATURE_DESCRIPTOR
                    + "\" is missing");
        }
        List<Annotation> annotations = annotationFeature.getValue();

        // create a new List, as removing many items from an existing one is terribly expensive
        // (unless we were using a LinkedList, what we do not want)
        List<Annotation> resultTokens = new ArrayList<Annotation>();
        for (Iterator<Annotation> tokenIterator = annotations.iterator(); tokenIterator.hasNext();) {
            Annotation annotation = tokenIterator.next();
            if (!remove(annotation)) {
                resultTokens.add(annotation);
            }
        }
        annotationFeature.setValue(resultTokens);
    }

}