package ws.palladian.kaggle.redhat.dataset;

import java.math.BigDecimal;
import java.util.Objects;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.split.TrainTestSplit;
import ws.palladian.core.value.NominalValue;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.functional.Filters;

public class PeopleIdSplit implements TrainTestSplit {

	private final class SplitFilter implements Filter<Instance> {
		@Override
		public boolean accept(Instance item) {
			// people_id can look like:
			// ppl_100
			// ppl_1e+05
			NominalValue peopleIdValue = (NominalValue) item.getVector().get("people_id");
			String[] split = peopleIdValue.getString().split("_");
			int peopleId = Integer.parseInt(new BigDecimal(split[1]).toPlainString());
			return peopleId % 2 == 0;
		}
	}

	private final Dataset dataset;

	public PeopleIdSplit(Dataset dataset) {
		this.dataset = Objects.requireNonNull(dataset, "dataset must not be null");
	}
	
	@Override
	public Dataset getTrain() {
		return dataset.subset((Factory<Filter<Instance>>) () -> new SplitFilter());
	}

	@Override
	public Dataset getTest() {
		return dataset.subset((Factory<Filter<Instance>>) () -> Filters.not(new SplitFilter()));
	}

}
