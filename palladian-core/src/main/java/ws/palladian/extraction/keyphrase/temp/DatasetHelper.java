package ws.palladian.extraction.keyphrase.temp;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

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
    
    private static final String DEFAULT_SEPARATOR = "#";

    private DatasetHelper() {
        // prevent instantiation.
    }

    public static Dataset2 loadDataset(final File filePath, final String separator) {
        if (!filePath.exists() || !filePath.isFile()) {
            throw new IllegalArgumentException(filePath.getAbsolutePath() + " does not exist or is no file.");
        }
        if (StringUtils.isEmpty(separator)) {
            throw new IllegalArgumentException("Separator must not be null or empty.");
        }
        
        final Dataset2 ret = new Dataset2(filePath);
        FileHelper.performActionOnEveryLine(filePath.getAbsolutePath(), new LineAction() {
            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split(separator);
                // FIXME build filename from filePath + file name in index file
                // File file = new File(split[0]);
                File file = new File(filePath.getParent(), split[0]);
                String[] categories;
                if (split.length > 1) {
                    categories = Arrays.copyOfRange(split, 1, split.length);
                } else {
                    categories = new String[0]; // no categories given.
                }
                ret.add(new DatasetItem(file, categories));
            }
        });
        return ret;
    }
    
    public static Dataset2 loadDataset(File filePath) {
        return loadDataset(filePath, DEFAULT_SEPARATOR);
    }

    /**
     * <p>
     * Return an {@link Iterator} for <a
     * href="http://en.wikipedia.org/wiki/Cross-validation_(statistics)#K-fold_cross-validation">K-fold
     * cross-validation</a>. The {@link Iterator} iterates over an Array with two {@link Dataset2} elements, the first
     * one meant for training, the second one for testing.
     * </p>
     * 
     * @param dataset The dataset to split.
     * @param folds The number of subsamples to create, i.e. <i>k</i> in statistics jargon.
     * @return An {@link Iterator} for for each fold, i.e. it contains the number of folds specified.
     */
    public static Iterator<Dataset2[]> crossValidate(Dataset2 dataset, int folds) {
        return new CrossValidationIterator(dataset, folds);
    }

    /**
     * <p>
     * An {@link Iterator} implementation for K-fold cross-validation. Each iteration returns a pair of {@link Dataset2}
     * s, the first one meant for training, the second one meant for testing.
     * </p>
     * 
     * @author Philipp Katz
     */
    private static final class CrossValidationIterator implements Iterator<Dataset2[]> {

        private final Dataset2 inputDataset;
        private final int folds;

        // sizes of all partitions
        private final int[] partitionSizes;

        // the current iteration
        private int iteration;

        // pointing at the start index used for training
        private int startIndex;

        public CrossValidationIterator(Dataset2 inputDataset, int folds) {
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
        public Dataset2[] next() {
            Dataset2 train = new Dataset2();
            Dataset2 test = new Dataset2();
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
            return new Dataset2[] {train, test};
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
