package ws.palladian.extraction.keyphrase.temp;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.io.LineAction;

/**
 * <p>
 * Helper class for working with datasets.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class DatasetHelper {

    private DatasetHelper() {
        // prevent instantiation.
    }

    public static Dataset loadDataset(File filePath, final String separator) {
        final Dataset ret = new Dataset();
        FileHelper.performActionOnEveryLine(filePath.getAbsolutePath(), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split(separator);
                if (split.length < 2) {
                    throw new IllegalStateException();
                }
                File file = new File(split[0]);
                String[] categories = Arrays.copyOfRange(split, 1, split.length);
                ret.add(new DatasetItem(file, categories));
            }
        });
        return ret;
    }

    /**
     * <p>
     * Return an {@link Iterator} for <a
     * href="http://en.wikipedia.org/wiki/Cross-validation_(statistics)#K-fold_cross-validation">K-fold
     * cross-validation</a>. The {@link Iterator} iterates over an Array with two {@link Dataset} elements, the first
     * one meant for training, the second one for testing.
     * </p>
     * 
     * @param dataset The dataset to split.
     * @param folds The number of subsamples to create, i.e. <i>k</i> in statistics jargon.
     * @return An {@link Iterator} for for each fold, i.e. it contains the number of folds specified.
     */
    public static Iterator<Dataset[]> crossValidate(Dataset dataset, int folds) {
        return new CrossValidationIterator(dataset, folds);
    }

    /**
     * <p>
     * An {@link Iterator} implementation for K-fold cross-validation. Each iteration returns a pair of {@link Dataset}
     * s, the first one meant for training, the second one meant for testing.
     * </p>
     * 
     * @author Philipp Katz
     */
    private static final class CrossValidationIterator implements Iterator<Dataset[]> {

        private final Dataset inputDataset;
        private final int folds;

        // sizes of all partitions
        private final int[] partitionSizes;

        // the current iteration
        private int iteration;

        // pointing at the start index used for training
        private int startIndex;

        public CrossValidationIterator(Dataset inputDataset, int folds) {
            this.inputDataset = inputDataset;
            this.folds = folds;
            this.iteration = 0;
            this.partitionSizes = calculatePartitionSizes(inputDataset.size(), folds);
        }

        @Override
        public boolean hasNext() {
            return iteration < folds;
        }

        @Override
        public Dataset[] next() {
            Dataset train = new Dataset();
            Dataset test = new Dataset();
            int endIndex = startIndex + partitionSizes[iteration];
            int index = 0;
            for (DatasetItem item : inputDataset) {
                if (index < startIndex || index >= endIndex) {
                    train.add(item);
                } else {
                    test.add(item);
                }
                index++;
            }
            startIndex += partitionSizes[iteration];
            iteration++;
            return new Dataset[] {train, test};
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }

    }

    /**
     * <p>
     * Calculate partition sizes of roughly same size, e.g. a dataset with 8 items is split into 3 of sizes 3, 3, and 2.
     * Package private to allow better unit testing.
     * </p>
     * 
     * @param size
     * @param k
     * @return
     */
    static int[] calculatePartitionSizes(int size, int k) {
        int division = size / k;
        int rest = size % k;
        int[] partitions = new int[k];
        for (int i = 0; i < k; i++) {
            partitions[i] = division;
            if (i < rest) {
                partitions[i]++;
            }
        }
        return partitions;
    }

}
