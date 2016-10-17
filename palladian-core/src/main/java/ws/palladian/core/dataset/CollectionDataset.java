package ws.palladian.core.dataset;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.ImmutableInstance;
import ws.palladian.core.Instance;
import ws.palladian.core.featurevector.FlyweightVectorSchema;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.CloseableIteratorAdapter;

public class CollectionDataset extends AbstractDataset {

	private final List<Instance> instances;
	private final FeatureInformation featureInformation;

	public CollectionDataset(Dataset dataset) {
		Objects.requireNonNull(dataset, "dataset was null");
		featureInformation = dataset.getFeatureInformation();
		instances = new ArrayList<>();
		FlyweightVectorSchema schema = new FlyweightVectorSchema(dataset.getFeatureInformation());
		for (Instance instance : dataset) {
			// on creation, copy the instances, to get rid of no longer required junk
			FeatureVector vector = schema.builder().set(instance.getVector()).create();
			instances.add(new ImmutableInstance(vector, instance.getCategory()));
		}
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
	
	public void sort(Comparator<? super Instance> comparator) {
		Collections.sort(instances, comparator);
	}

}
