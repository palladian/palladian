package ws.palladian.core.dataset;

import ws.palladian.core.Instance;
import ws.palladian.helper.collection.CompositeIterator;
import ws.palladian.helper.io.CloseableIterator;

import java.io.IOException;
import java.util.*;

public class ConcatenatedDataset extends AbstractDataset {

    private static final class ConcatenatedIterator extends CompositeIterator<Instance> implements CloseableIterator<Instance> {

        private final List<CloseableIterator<Instance>> iterators;

        public ConcatenatedIterator(List<CloseableIterator<Instance>> iterators) {
            super(iterators);
            this.iterators = iterators;
        }

        @Override
        public void close() throws IOException {
            for (CloseableIterator<Instance> iterator : iterators) {
                try {
                    iterator.close();
                } catch (IOException e) {
                    // ignore
                }
            }
        }

    }

    private final List<Dataset> datasets;

    public ConcatenatedDataset(Dataset... datasets) {
        this(Arrays.asList(datasets));
    }

    public ConcatenatedDataset(Collection<Dataset> datasets) {
        FeatureInformation featureInformation = null;
        for (Dataset dataset : datasets) {
            if (featureInformation == null) {
                featureInformation = dataset.getFeatureInformation();
            } else if (!featureInformation.equals(dataset.getFeatureInformation())) {
                throw new IllegalArgumentException("datasets are not compatible");
            }
        }
        this.datasets = new ArrayList<>(Objects.requireNonNull(datasets));
    }

    @Override
    public CloseableIterator<Instance> iterator() {
        List<CloseableIterator<Instance>> iterators = new ArrayList<>();
        for (Dataset dataset : datasets) {
            iterators.add(dataset.iterator());
        }
        return new ConcatenatedIterator(iterators);
    }

    @Override
    public FeatureInformation getFeatureInformation() {
        return datasets.iterator().next().getFeatureInformation();
    }

    @Override
    public long size() {
        long size = 0;
        for (Dataset dataset : datasets) {
            size += dataset.size();
        }
        return size;
    }

}
