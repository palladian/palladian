package ws.palladian.core.dataset;

import java.io.IOException;

import ws.palladian.core.Instance;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.io.CloseableIterator;

class SubDataset extends AbstractDataset {

	private static final class SubDatasetIterator extends AbstractIterator<Instance> implements CloseableIterator<Instance> {

		private final CloseableIterator<Instance> iterator;
		private final Filter<? super Instance> instanceFilter;

		public SubDatasetIterator(CloseableIterator<Instance> iterator, Filter<? super Instance> instanceFilter) {
			this.iterator = iterator;
			this.instanceFilter = instanceFilter;
		}

		@Override
		protected Instance getNext() throws Finished {
			while (iterator.hasNext()) {
				Instance next = iterator.next();
				if (instanceFilter.accept(next)) {
					return next;
				}
			}
			throw FINISHED;
		}

		@Override
		public void close() throws IOException {
			iterator.close();
		}

	}

	private final Dataset dataset;
	private final Factory<? extends Filter<? super Instance>> instanceFilterFactory;

	/** Instantiated from {@link AbstractDataset}; not used from outside. */
	SubDataset(Dataset dataset, Factory<? extends Filter<? super Instance>> instanceFilterFactory) {
		this.dataset = dataset;
		this.instanceFilterFactory = instanceFilterFactory;
	}

	@Override
	public CloseableIterator<Instance> iterator() {
		return new SubDatasetIterator(dataset.iterator(), instanceFilterFactory.create());
	}

	@Override
	public FeatureInformation getFeatureInformation() {
		return dataset.getFeatureInformation();
	}

	@Override
	public long size() {
		return CollectionHelper.count(iterator());
	}

}
