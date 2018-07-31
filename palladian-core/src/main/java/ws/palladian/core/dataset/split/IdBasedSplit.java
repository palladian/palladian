package ws.palladian.core.dataset.split;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import java.util.function.Predicate;

public class IdBasedSplit extends AbstractFilterSplit {

	public IdBasedSplit(Dataset dataset) {
		super(dataset);
	}

	@Override
	protected Predicate<? super Instance> createFilter() {
		return new Predicate<Instance>() {
			@Override
			public boolean test(Instance item) {
				String id = item.getVector().getNominal("Id").getString();
				return Integer.parseInt(id) % 2 == 0;
			}
		};
	}

}
