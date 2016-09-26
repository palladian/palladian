package ws.palladian.core.dataset.split;

import static ws.palladian.helper.functional.Filters.not;

import java.util.Objects;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;

/**
 * Template for {@link TrainTestSplit} based on a {@link Filter}. The filter
 * decides which data goes into the training and which into the testing set.
 * 
 * @author pk
 *
 */
public abstract class AbstractFilterSplit implements TrainTestSplit {

	private final Dataset dataset;

	protected AbstractFilterSplit(Dataset dataset) {
		this.dataset = Objects.requireNonNull(dataset, "dataset must not be null");
	}

	@Override
	public final Dataset getTrain() {
		return dataset.subset(new Factory<Filter<? super Instance>>() {
			@Override
			public Filter<? super Instance> create() {
				return createFilter();
			}
		});
	}

	@Override
	public final Dataset getTest() {
		return dataset.subset(new Factory<Filter<? super Instance>>() {
			@Override
			public Filter<? super Instance> create() {
				return not(createFilter());
			}
		});
	}

	/**
	 * Subclasses return the filter which accepts items to go to the training
	 * set; items which are not accepted by the filter, go to the test set.
	 * 
	 * @return The filter for splitting the data.
	 */
	protected abstract Filter<? super Instance> createFilter();

}
