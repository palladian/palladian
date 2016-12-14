package ws.palladian.core.dataset;

import java.io.IOException;

import ws.palladian.core.FilteredVector;
import ws.palladian.core.ImmutableInstance;
import ws.palladian.core.Instance;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.AbstractIterator2;
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

	private final class DatasetWithFeatureAsCategoryIterator extends AbstractIterator2<Instance>
			implements CloseableIterator<Instance> {

		private final CloseableIterator<Instance> iterator;

		public DatasetWithFeatureAsCategoryIterator(CloseableIterator<Instance> iterator) {
			this.iterator = iterator;
		}

		@Override
		protected Instance getNext() {
			if (iterator.hasNext()) {
				Instance next = iterator.next();
				FilteredVector filteredVector = new FilteredVector(next.getVector(), featureInformation.getFeatureNames());
				
				Value value = next.getVector().get(featureName);
				if (value == null) {
					throw new IllegalArgumentException("No feature with name \"" + featureName + "\".");
				}
				String category = value.isNull() ? Instance.NO_CATEGORY_DUMMY : value.toString();
				return new ImmutableInstance(filteredVector, category);

			}
			return finished();
		}

		@Override
		public void close() throws IOException {
			iterator.close();
		}

	}

	private final Dataset dataset;
	private final String featureName;
	private final FeatureInformation featureInformation;

	public DatasetWithFeatureAsCategory(Dataset dataset, String featureName) {
		this.dataset = dataset;
		this.featureName = featureName;
		this.featureInformation = new FeatureInformationBuilder().add(dataset.getFeatureInformation()).remove(featureName).create();
	}

	@Override
	public CloseableIterator<Instance> iterator() {
		return new DatasetWithFeatureAsCategoryIterator(dataset.iterator());
	}
	
	@Override
	public FeatureInformation getFeatureInformation() {
		return featureInformation;
	}

	@Override
	public long size() {
		return dataset.size();
	}

}
