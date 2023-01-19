package ws.palladian.core.dataset;

import ws.palladian.core.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.CloseableIteratorAdapter;

public final class DefaultDataset extends AbstractDataset {

    private final Iterable<? extends Instance> instances;

    private FeatureInformation featureInformation;

    private int size = -1;

    public DefaultDataset(Iterable<? extends Instance> instances) {
        this.instances = instances;
    }

    @Override
    public CloseableIterator<Instance> iterator() {
        return new CloseableIteratorAdapter<>(instances.iterator());
    }

    @Override
    public FeatureInformation getFeatureInformation() {
        if (featureInformation == null) {
            featureInformation = FeatureInformationBuilder.fromInstances(instances).create();
        }
        return featureInformation;
    }

    @Override
    public long size() {
        if (size == -1) {
            size = CollectionHelper.count(iterator());
        }
        return size;
    }

}
