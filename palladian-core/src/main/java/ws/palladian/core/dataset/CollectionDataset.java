package ws.palladian.core.dataset;

import java.util.List;

import ws.palladian.core.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.CloseableIteratorAdapter;

public class CollectionDataset extends AbstractDataset {

	private final List<Instance> instances;
	private final FeatureInformation featureInformation;

	public CollectionDataset(Dataset dataset) {
		featureInformation = dataset.getFeatureInformation();
		instances = CollectionHelper.newArrayList(dataset.iterator());
	}

	@Override
	public CloseableIterator<Instance> iterator() {
		return new CloseableIteratorAdapter<>(CollectionHelper.unmodifiableIterator(instances.iterator()));
	}

	@Override
	public FeatureInformation getFeatureInformation() {
		return featureInformation;
	}

	@Override
	public long size() {
		return instances.size();
	}
	
	@Override
	public Dataset buffer() {
		return this; // already buffered
	}

}
