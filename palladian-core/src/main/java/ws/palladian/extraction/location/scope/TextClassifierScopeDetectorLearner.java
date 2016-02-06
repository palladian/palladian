package ws.palladian.extraction.location.scope;

import java.util.Iterator;

import ws.palladian.extraction.location.evaluation.LocationDocument;
import ws.palladian.extraction.location.scope.DictionaryScopeDetector.DictionaryScopeDetectorLearner;

/**
 * A learner which learns a text-classification-based model from located text documents. The model can then be used by
 * the corresponding scope detectors to peform predictions.
 * 
 * @author Philipp Katz
 * @see DictionaryScopeDetectorLearner
 * @see NearestNeighborScopeDetectorLearner
 */
public interface TextClassifierScopeDetectorLearner {
    /**
     * Train a new model for location scope detection. The dataset is represented by the {@link Iterator}.
     * 
     * @param documentIterator The iterator representing the dataset, not <code>null</code>.
     * @return The created model.
     */
    TextClassifierScopeModel train(Iterable<? extends LocationDocument> documentIterator);
}
