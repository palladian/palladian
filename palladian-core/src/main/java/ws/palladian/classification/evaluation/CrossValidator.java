package ws.palladian.classification.evaluation;

import ws.palladian.classification.evaluation.CrossValidator.Fold;
import ws.palladian.core.dataset.split.TrainTestSplit;

/**
 * A cross validator provides multiple splits over a given dataset, which can be
 * iterated sequentially. The individual folds split the dataset in a training
 * and validation/test set.
 * 
 * @author pk
 * @see <a href=
 *      "https://en.wikipedia.org/wiki/Cross-validation_(statistics)">Cross-validation
 *      on Wikipedia</a>
 */
public interface CrossValidator extends Iterable<Fold> {

	interface Fold extends TrainTestSplit {
		int getFold();
	}

	/**
	 * @return The number of folds performed by this cross validator.
	 */
	int getNumFolds();

}
