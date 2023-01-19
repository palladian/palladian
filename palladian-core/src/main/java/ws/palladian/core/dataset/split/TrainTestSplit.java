package ws.palladian.core.dataset.split;

import ws.palladian.core.dataset.Dataset;

/**
 * Describes a split in training and testing/validation data.
 *
 * @author Philipp Katz
 */
public interface TrainTestSplit {

    /**
     * @return The training set.
     */
    Dataset getTrain();

    /**
     * @return The testing/validation set.
     */
    Dataset getTest();

}
