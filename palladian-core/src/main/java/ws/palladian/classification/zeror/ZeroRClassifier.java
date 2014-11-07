package ws.palladian.classification.zeror;

import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntriesBuilder;
import ws.palladian.classification.Classifier;
import ws.palladian.processing.Classifiable;

/**
 * <p>
 * Baseline classifier which does not consider any features but just predicts the most common class from training.
 * </p>
 * 
 * @author pk
 * @see <a href="http://www.saedsayad.com/zeror.htm">ZeroR</a>
 */
public final class ZeroRClassifier implements Classifier<ZeroRModel> {

    @Override
    public CategoryEntries classify(Classifiable classifiable, ZeroRModel model) {
        return new CategoryEntriesBuilder(model.getCategoryProbabilities()).create();
    }

}
