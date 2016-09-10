package ws.palladian.core.dataset.split;


import static ws.palladian.helper.functional.Filters.not;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;

public class RandomSplit implements TrainTestSplit {
	
	/** The logger for this class. */
	private static final Logger LOGGER = LoggerFactory.getLogger(RandomSplit.class);

	private final class SplitAssignmentFilter implements Filter<Object> {
		private int currentIndex;

		@Override
		public boolean accept(Object item) {
			return indices.get(currentIndex++) < splitIndex;
		}
	}

	private final Dataset dataset;
	private final List<Integer> indices;
	private final int splitIndex;

	public RandomSplit(Dataset dataset, double trainPercentage, Random random) {
		this.dataset = Objects.requireNonNull(dataset, "dataset must not be null");
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
	public Dataset getTrain() {
		return dataset.subset(new Factory<Filter<Object>>() {
			@Override
			public Filter<Object> create() {
				return new SplitAssignmentFilter();
			}
		});
	}

	@Override
	public Dataset getTest() {
		return dataset.subset(new Factory<Filter<Object>>() {
			@Override
			public Filter<Object> create() {
				return not(new SplitAssignmentFilter());
			}
		});
	}

}
