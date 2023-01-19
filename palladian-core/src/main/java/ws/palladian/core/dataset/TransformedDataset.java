package ws.palladian.core.dataset;

import ws.palladian.core.Instance;
import ws.palladian.helper.collection.AbstractIterator2;
import ws.palladian.helper.io.CloseableIterator;

import java.io.IOException;

class TransformedDataset extends AbstractDataset implements Dataset {

    private static final class TransformedDatasetIterator extends AbstractIterator2<Instance> implements CloseableIterator<Instance> {

        private final CloseableIterator<Instance> iterator;
        private final DatasetTransformer transformer;

        private TransformedDatasetIterator(CloseableIterator<Instance> iterator, DatasetTransformer transformer) {
            this.iterator = iterator;
            this.transformer = transformer;
        }

        @Override
        public void close() throws IOException {
            iterator.close();
        }

        @Override
        protected Instance getNext() {
            if (iterator.hasNext()) {
                return transformer.apply(iterator.next());
            }
            return finished();
        }

    }

    private final Dataset dataset;
    private final DatasetTransformer transformer;

    /** Instantiated from {@link AbstractDataset}; not used from outside. */
    TransformedDataset(Dataset dataset, DatasetTransformer transformer) {
        this.dataset = dataset;
        this.transformer = transformer;
    }

    @Override
    public final CloseableIterator<Instance> iterator() {
        return new TransformedDatasetIterator(dataset.iterator(), transformer);
    }

    @Override
    public FeatureInformation getFeatureInformation() {
        return transformer.getFeatureInformation(dataset.getFeatureInformation());
    }

    @Override
    public final long size() {
        return dataset.size();
    }

}
