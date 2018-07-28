package ws.palladian.core.dataset.split;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.split.AbstractFilterSplit;
import ws.palladian.helper.functional.Filter;

public class IdBasedSplit extends AbstractFilterSplit {

	public IdBasedSplit(Dataset dataset) {
		super(dataset);
	}

	@Override
	protected Filter<? super Instance> createFilter() {
		return new Filter<Instance>() {
			@Override
			public boolean accept(Instance item) {
				String id = item.getVector().getNominal("Id").getString();
				return Integer.parseInt(id) % 2 == 0;
			}
		};
	}

}
