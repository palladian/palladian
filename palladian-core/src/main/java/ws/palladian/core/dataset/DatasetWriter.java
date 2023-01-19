package ws.palladian.core.dataset;

import ws.palladian.helper.ProgressReporter;

/**
 * Defines writers which are used to write a {@link Dataset} e.g. to a file.
 *
 * @author Philipp Katz
 */
public interface DatasetWriter {

    /**
     * Write the given dataset.
     *
     * @param dataset The dataset.
     */
    void write(Dataset dataset);

    /**
     * Write the given dataset.
     *
     * @param dataset  The dataset.
     * @param progress Write progress reporter.
     */
    void write(Dataset dataset, ProgressReporter progress);

    /**
     * Get an appender object which can be used for writing the dataset step by
     * step, e.g. within in loops.
     *
     * @param featureInformation The description of the dataset.
     * @return An appender used for writing the single instances.
     */
    DatasetAppender write(FeatureInformation featureInformation);

}
