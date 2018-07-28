//package ws.palladian.kaggle.redhat.dataset.sparse;
//
//import java.util.ArrayList;
//import java.util.Iterator;
//import java.util.List;
//import java.util.Map;
//
//import gnu.trove.iterator.TIntIterator;
//import gnu.trove.list.array.TIntArrayList;
//import ws.palladian.core.Instance;
//import ws.palladian.core.InstanceBuilder;
//import ws.palladian.core.dataset.AbstractDataset;
//import ws.palladian.core.dataset.Dataset;
//import ws.palladian.core.dataset.FeatureInformation;
//import ws.palladian.core.value.Value;
//import ws.palladian.helper.collection.AbstractIterator2;
//import ws.palladian.helper.collection.CollectionHelper;
//import ws.palladian.helper.collection.Vector.VectorEntry;
//import ws.palladian.helper.io.CloseableIterator;
//import ws.palladian.helper.io.CloseableIteratorAdapter;
//
//class SparseBufferedDataset extends AbstractDataset implements Dataset {
//
//	private final FeatureInformation featureInformation;
//
//	/** Subsequent category names of all instances. */
//	private final List<String> categories;
//
//	/** Subsequent, non-null values of all instances. */
//	private final List<Value> values;
//
//	/** Feature index of the values. */
//	private final TIntArrayList index;
//
//	/** Beginning of new Instance in {@link #values} or {@link #index}. */
//	private final TIntArrayList headers;
//
//	private final List<String> featureNames;
//
//	SparseBufferedDataset(SparseDatasetReader sparseDataset) {
//		featureInformation = sparseDataset.getFeatureInformation();
//		categories = new ArrayList<>();
//		values = new ArrayList<>();
//		index = new TIntArrayList();
//		headers = new TIntArrayList();
//
//		featureNames = new ArrayList<>(sparseDataset.getFeatureInformation().getFeatureNames());
//		Map<String, Integer> featureIndices = CollectionHelper.createIndexMap(featureNames);
//
//		int rowheader = 0;
//		headers.add(rowheader);
//		for (Instance instance : sparseDataset) {
//			categories.add(instance.getCategory());
//			for (VectorEntry<String, Value> vectorEntry : instance.getVector()) {
//				Value value = vectorEntry.value();
//				if (!value.isNull()) {
//					Integer featureIndex = featureIndices.get(vectorEntry.key());
//					values.add(value);
//					index.add(featureIndex);
//					rowheader++;
//				}
//			}
//			headers.add(rowheader);
//		}
//	}
//
//	@Override
//	public CloseableIterator<Instance> iterator() {
//
//		AbstractIterator2<Instance> iterator = new AbstractIterator2<Instance>() {
//			final Iterator<Value> valuesIterator = values.iterator();
//			final TIntIterator headersIterator = headers.iterator();
//			final Iterator<String> categoriesIterator = categories.iterator();
//			final TIntIterator indexIterator = index.iterator();
//			int previous = headersIterator.next();
//
//			@Override
//			protected Instance getNext() {
//				if (!headersIterator.hasNext()) {
//					return finished();
//				}
//				InstanceBuilder builder = new InstanceBuilder();
//				int header = headersIterator.next();
//				for (int i = 0; i < header - previous; i++) {
//					Value value = valuesIterator.next();
//					int index = indexIterator.next();
//					String name = featureNames.get(index);
//					builder.set(name, value);
//				}
//				previous = header;
//				String label = categoriesIterator.next();
//				return builder.create(label);
//			}
//		};
//		return new CloseableIteratorAdapter<>(iterator);
//	}
//
//	@Override
//	public FeatureInformation getFeatureInformation() {
//		return featureInformation;
//	}
//
//	@Override
//	public long size() {
//		return categories.size();
//	}
//	
//	@Override
//	public Dataset buffer() {
//		return this;
//	}
//
//}
