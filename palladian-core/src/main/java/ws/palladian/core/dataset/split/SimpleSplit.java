package ws.palladian.core.dataset.split;

import ws.palladian.core.dataset.Dataset;

import java.util.Objects;

/**
 * Wraps a separate training and test set into a single split.
 *
 * @author Philipp Katz
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
