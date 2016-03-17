package ws.palladian.core.dataset;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import ws.palladian.core.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.CloseableIteratorAdapter;

public class CollectionDataset extends AbstractDataset {

	private final List<Instance> instances;
	private final Set<String> featureNames;

	public CollectionDataset(Dataset dataset) {
		featureNames = dataset.getFeatureNames();
		instances = CollectionHelper.newArrayList(dataset.iterator());
	}

	@Override
	public CloseableIterator<Instance> iterator() {
		return new CloseableIteratorAdapter<>(CollectionHelper.unmodifiableIterator(instances.iterator()));
	}

	@Override
	public Set<String> getFeatureNames() {
		return Collections.unmodifiableSet(featureNames);
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
