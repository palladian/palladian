package ws.palladian.core.dataset;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;

import ws.palladian.core.AppendedVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.ImmutableInstance;
import ws.palladian.core.Instance;
import ws.palladian.core.dataset.AbstractDataset;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.FileHelper;

public class AppendedDataset extends AbstractDataset {

	private static final class AppendedDatasetIterator extends AbstractIterator2<Instance>
			implements CloseableIterator<Instance> {
		private final List<CloseableIterator<Instance>> iterators;

		AppendedDatasetIterator(List<CloseableIterator<Instance>> iterators) {
			this.iterators = iterators;
		}

		@Override
		protected Instance getNext() {
			if (!iterators.get(0).hasNext()) {
				return finished();
			}
			List<FeatureVector> vectors = new ArrayList<>();
			String category = null;
			for (Iterator<Instance> iterator : iterators) {
				Instance instance = iterator.next();
				vectors.add(instance.getVector());
				category = instance.getCategory();
			}
			return new ImmutableInstance(new AppendedVector(vectors), category);
		}

		@Override
		public void close() throws IOException {
			FileHelper.close(iterators.toArray(new Closeable[0]));
		}
	}

	private final List<Dataset> datasets;

	public AppendedDataset(Dataset... datasets) {
		this.datasets = Arrays.asList(Objects.requireNonNull(datasets));
	}

	@Override
	public CloseableIterator<Instance> iterator() {
		return new AppendedDatasetIterator(createIterators());
	}

	private List<CloseableIterator<Instance>> createIterators() {
		List<CloseableIterator<Instance>> iterators = new ArrayList<>();
		for (Dataset dataset : datasets) {
			iterators.add(dataset.iterator());
		}
		return iterators;
	}

	@Override
	public FeatureInformation getFeatureInformation() {
		FeatureInformationBuilder builder = new FeatureInformationBuilder();
		for (Dataset dataset : datasets) {
			builder.add(dataset.getFeatureInformation());
		}
		return builder.create();
	}

	@Override
	public long size() {
		return datasets.get(0).size();
	}

}
