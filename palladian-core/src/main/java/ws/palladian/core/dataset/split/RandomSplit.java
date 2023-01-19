package ws.palladian.core.dataset.split;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.collection.CollectionHelper;

import java.util.*;
import java.util.function.Predicate;

public class RandomSplit extends AbstractFilterSplit {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RandomSplit.class);

    private final class SplitAssignmentFilter implements Predicate<Object> {
        private int currentIndex;

        @Override
        public boolean test(Object item) {
            return indices.get(currentIndex++) < splitIndex;
        }
    }

    private final List<Integer> indices;
    private final int splitIndex;

    public RandomSplit(Dataset dataset, double trainPercentage, Random random) {
        super(dataset);
        if (trainPercentage <= 0 || trainPercentage > 1) {
            throw new IllegalArgumentException("trainPercentage must be in range (0,1]");
        }
        Objects.requireNonNull(random, "random must not be null");
        int numInstances = CollectionHelper.count(dataset.iterator());
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < numInstances; i++) {
            indices.add(i);
        }
        Collections.shuffle(indices, random);
        this.indices = Collections.unmodifiableList(indices);
        this.splitIndex = (int) Math.round(trainPercentage * numInstances);
        LOGGER.debug("numInstances = {}", numInstances);
        LOGGER.debug("splitIndex = {}", splitIndex);
    }

    public RandomSplit(Dataset dataset, double trainPercentage) {
        this(dataset, trainPercentage, new Random());
    }

    @Override
    protected Predicate<? super Instance> createFilter() {
        return new SplitAssignmentFilter();
    }

}
