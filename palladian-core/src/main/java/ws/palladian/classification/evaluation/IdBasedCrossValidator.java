package ws.palladian.classification.evaluation;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.collection.AbstractIterator2;

import java.util.Iterator;
import java.util.Objects;
import java.util.function.Predicate;

import static ws.palladian.helper.functional.Predicates.not;

/**
 * Allows cross-validating a {@link Dataset} based on an ID value. The ID value
 * has to contain an integer number. The split is performed by taking the
 * modulus.
 *
 * @author Philipp Katz
 */
public class IdBasedCrossValidator implements CrossValidator {

    private final class FilterImplementation implements Predicate<Instance> {
        private final int fold;

        FilterImplementation(int fold) {
            this.fold = fold;
        }

        @Override
        public boolean test(Instance item) {
            // String id = item.getVector().getNominal(idValueName).getString();
            String id = item.getVector().get(idValueName).toString();
            return Integer.parseInt(id) % numFolds == fold;
        }
    }

    private final class FoldImplementation implements Fold {
        private final int fold;

        FoldImplementation(int fold) {
            this.fold = fold;
        }

        @Override
        public Dataset getTrain() {
            return dataset.subset(not(new FilterImplementation(fold)));
        }

        @Override
        public Dataset getTest() {
            return dataset.subset((new FilterImplementation(fold)));
        }

        @Override
        public int getFold() {
            return fold;
        }
    }

    private static final int DEFAULT_NUM_FOLDS = 5;
    private final Dataset dataset;
    private final int numFolds;
    private String idValueName;

    /**
     * Create a new instance.
     *
     * @param dataset     The dataset.
     * @param numFolds    The number of folds.
     * @param idValueName The name of the value which contains the identifier used for
     *                    splitting.
     */
    public IdBasedCrossValidator(Dataset dataset, int numFolds, String idValueName) {
        this.dataset = Objects.requireNonNull(dataset, "dataset must not be null");
        if (numFolds < 2) {
            throw new IllegalArgumentException("numFolds must be at least 2");
        }
        this.numFolds = numFolds;
        this.idValueName = Objects.requireNonNull(idValueName, "idValueName mut not be null");
    }

    public IdBasedCrossValidator(Dataset dataset, String idValueName) {
        this(dataset, DEFAULT_NUM_FOLDS, idValueName);
    }

    @Override
    public Iterator<Fold> iterator() {
        return new AbstractIterator2<Fold>() {
            int fold = 0;

            @Override
            protected Fold getNext() {
                if (fold < numFolds) {
                    return new FoldImplementation(fold++);
                }
                return finished();
            }
        };
    }

    @Override
    public int getNumFolds() {
        return numFolds;
    }

}
