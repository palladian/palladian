package ws.palladian.core.dataset;

import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.CloseableIteratorAdapter;

public final class RandomDataset extends AbstractDataset {

	private final int size;

	public RandomDataset(int size) {
		this.size = size;
	}

	@Override
	public CloseableIterator<Instance> iterator() {

		AbstractIterator2<Instance> iterator = new AbstractIterator2<Instance>() {
			int index = 0;

			@Override
			protected Instance getNext() {
				if (index++ == size) {
					return finished();
				}
				return new InstanceBuilder().set("index", index).create(index % 2 == 0);
			}

		};
		return new CloseableIteratorAdapter<>(iterator);
	}

	@Override
	public FeatureInformation getFeatureInformation() {
		return new FeatureInformationBuilder().set("index", ImmutableIntegerValue.class).create();
	}

	@Override
	public long size() {
		return size;
	}

}
