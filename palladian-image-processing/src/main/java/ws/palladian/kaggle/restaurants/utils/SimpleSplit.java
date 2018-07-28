package ws.palladian.kaggle.restaurants.utils;

import java.util.Objects;

import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.split.TrainTestSplit;

/**
 * Wraps a separate training and test set into a single split.
 * 
 * @author pk
 */
public final class SimpleSplit implements TrainTestSplit {

	private final Dataset trainSet;
	private final Dataset testSet;

	public SimpleSplit(Dataset trainSet, Dataset testSet) {
		this.trainSet = Objects.requireNonNull(trainSet);
		this.testSet = Objects.requireNonNull(testSet);
	}

	@Override
	public Dataset getTrain() {
		return trainSet;
	}

	@Override
	public Dataset getTest() {
		return testSet;
	}

}
