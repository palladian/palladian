package ws.palladian.core.dataset;

import java.io.IOException;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.FilteredVector;
import ws.palladian.core.ImmutableInstance;
import ws.palladian.core.Instance;
import ws.palladian.helper.collection.AbstractIterator2;
import java.util.function.Predicate;
import ws.palladian.helper.io.CloseableIterator;

/**
 * A dataset, where some features can be removed by applying a filter.
 * 
 * @author pk
 */
public class FilteredDataset extends AbstractDataset {

	private final class FilteredDatasetIterator extends AbstractIterator2<Instance>
			implements CloseableIterator<Instance> {

		private final CloseableIterator<Instance> original = FilteredDataset.this.original.iterator();

		@Override
		public void close() throws IOException {
			original.close();
		}

		@Override
		protected Instance getNext() {
			if (original.hasNext()) {
				Instance current = original.next();
				FeatureVector filteredVector = new FilteredVector(current.getVector(), featureInformation.getFeatureNames());
				return new ImmutableInstance(filteredVector, current.getCategory());
			}
			return finished();
		}

	}

	private final Dataset original;

	private final FeatureInformation featureInformation;

	public FilteredDataset(Dataset original, Predicate<? super String> filteredFeatures) {
		this.original = original;
		this.featureInformation = new FeatureInformationBuilder().add(original.getFeatureInformation()).filter(filteredFeatures).create();
	}

	@Override
	public CloseableIterator<Instance> iterator() {
		return new FilteredDatasetIterator();
	}
	
	@Override
	public FeatureInformation getFeatureInformation() {
		return featureInformation;
	}

	@Override
	public long size() {
		return original.size();
	}

}
