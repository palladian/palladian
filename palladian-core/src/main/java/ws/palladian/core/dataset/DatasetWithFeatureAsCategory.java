package ws.palladian.core.dataset;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import ws.palladian.core.FilteredVector;
import ws.palladian.core.ImmutableInstance;
import ws.palladian.core.Instance;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.io.CloseableIterator;

/**
 * Allows to use a specific feature from the feature vector as category (i.e.
 * classification target). The specified feature will be converted to the
 * {@link Instance}'s {@link Instance#getCategory()}, and will be removed from
 * the {@link Instance#getVector()}.
 * 
 * @author pk
 */
public class DatasetWithFeatureAsCategory extends AbstractDataset {

	private final class DatasetWithFeatureAsCategoryIterator extends AbstractIterator<Instance>
			implements CloseableIterator<Instance> {

		private final CloseableIterator<Instance> iterator;

		public DatasetWithFeatureAsCategoryIterator(CloseableIterator<Instance> iterator) {
			this.iterator = iterator;
		}

		@Override
		protected Instance getNext() throws AbstractIterator.Finished {
			if (iterator.hasNext()) {
				Instance next = iterator.next();
				FilteredVector filteredVector = new FilteredVector(next.getVector(), featureNames);
				Value category = next.getVector().get(featureName);
				if (category == null) {
					throw new IllegalArgumentException("No feature with name \"" + featureName + "\".");
				}
				if (category == NullValue.NULL) {
					throw new IllegalArgumentException("Feature is NULL");
				}
				return new ImmutableInstance(filteredVector, category.toString());

			}
			throw FINISHED;
		}

		@Override
		public void close() throws IOException {
			iterator.close();
		}

	}

	private final Dataset dataset;
	private final String featureName;
	private final Set<String> featureNames;

	public DatasetWithFeatureAsCategory(Dataset dataset, String featureName) {
		this.dataset = dataset;
		this.featureName = featureName;
		this.featureNames = new HashSet<>(dataset.getFeatureNames());
		this.featureNames.remove(featureName);
	}

	@Override
	public CloseableIterator<Instance> iterator() {
		return new DatasetWithFeatureAsCategoryIterator(dataset.iterator());
	}

	@Override
	public Set<String> getFeatureNames() {
		return Collections.unmodifiableSet(featureNames);
	}

	@Override
	public long size() {
		return dataset.size();
	}

}
