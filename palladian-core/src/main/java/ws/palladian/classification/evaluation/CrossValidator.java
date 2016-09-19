package ws.palladian.classification.evaluation;

import ws.palladian.classification.evaluation.CrossValidator.Fold;
import ws.palladian.core.dataset.split.TrainTestSplit;

public interface CrossValidator extends Iterable<Fold> {

	interface Fold extends TrainTestSplit {
		int getFold();
	}

}
