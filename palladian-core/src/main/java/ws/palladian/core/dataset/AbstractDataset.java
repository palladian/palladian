package ws.palladian.core.dataset;

import java.util.Objects;

import ws.palladian.core.Instance;
import ws.palladian.helper.functional.Filter;

public abstract class AbstractDataset implements Dataset {

	@Override
	public Dataset filterFeatures(Filter<? super String> nameFilter) {
		Objects.requireNonNull(nameFilter, "nameFilter must not be null");
		return new FilteredDataset(this, nameFilter);
	}

	@Override
	public Dataset subset(Filter<? super Instance> instanceFilter) {
		Objects.requireNonNull(instanceFilter, "instanceFilter must not be null");
		return new SubDataset(this, instanceFilter);
	}

}
