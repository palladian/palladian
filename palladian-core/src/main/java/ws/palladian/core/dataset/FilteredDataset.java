package ws.palladian.core.dataset;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.FilteredVector;
import ws.palladian.core.ImmutableInstance;
import ws.palladian.core.Instance;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Filter;
import ws.palladian.helper.io.CloseableIterator;

/**
 * A dataset, where some features can be removed by applying a filter.
 * 
 * @author pk
 */
public class FilteredDataset extends AbstractDataset {

	private final class FilteredDatasetIterator extends AbstractIterator<Instance>
			implements CloseableIterator<Instance> {

		private final CloseableIterator<Instance> original = FilteredDataset.this.original.iterator();

		@Override
		public void close() throws IOException {
			original.close();
		}

		@Override
		protected Instance getNext() throws Finished {
			if (original.hasNext()) {
				Instance current = original.next();
				FeatureVector filteredVector = new FilteredVector(current.getVector(), filteredNames);
				return new ImmutableInstance(filteredVector, current.getCategory());
			}
			throw FINISHED;
		}

	}

	private final Dataset original;

	private final Set<String> filteredNames;

	public FilteredDataset(Dataset original, Filter<? super String> filteredFeatures) {
		this.original = original;
		this.filteredNames = CollectionHelper.filterSet(original.getFeatureNames(), filteredFeatures);
	}

	@Override
	public CloseableIterator<Instance> iterator() {
		return new FilteredDatasetIterator();
	}

	@Override
	public Set<String> getFeatureNames() {
		return Collections.unmodifiableSet(filteredNames);
	}

	@Override
	public long size() {
		return original.size();
	}

}
