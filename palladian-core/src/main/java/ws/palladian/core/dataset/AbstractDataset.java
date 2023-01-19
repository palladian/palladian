package ws.palladian.core.dataset;

import ws.palladian.core.Instance;
import ws.palladian.helper.functional.Factories;
import ws.palladian.helper.functional.Factory;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.FileHelper;

import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

public abstract class AbstractDataset implements Dataset {

    @Override
    public Dataset filterFeatures(Predicate<? super String> nameFilter) {
        Objects.requireNonNull(nameFilter, "nameFilter must not be null");
        return new FilteredDataset(this, nameFilter);
    }

    @Override
    public Dataset subset(Predicate<? super Instance> instanceFilter) {
        Objects.requireNonNull(instanceFilter, "instanceFilter must not be null");
        return new SubDataset(this, Factories.constant(instanceFilter));
    }

    @Override
    public Dataset subset(Factory<? extends Predicate<? super Instance>> instanceFilterFactory) {
        Objects.requireNonNull(instanceFilterFactory, "instanceFilterFactory must not be null");
        return new SubDataset(this, instanceFilterFactory);
    }

    @Override
    public Dataset buffer() {
        return new CollectionDataset(this);
    }

    @Override
    public final Set<String> getFeatureNames() {
        return getFeatureInformation().getFeatureNames();
    }

    @Override
    public Dataset transform(DatasetTransformer transformer) {
        Objects.requireNonNull(transformer, "transformer must not be null");
        return new TransformedDataset(this, transformer);
    }

    // hashCode + equals

    @Override
    public int hashCode() {
        int hashCode = 1;
        for (Instance instance : this) {
            hashCode = 31 * hashCode + instance.hashCode();
        }
        return hashCode;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        AbstractDataset other = (AbstractDataset) obj;
        if (size() != other.size()) {
            return false;
        }
        if (!getFeatureInformation().equals(other.getFeatureInformation())) {
            return false;
        }
        CloseableIterator<Instance> iterator1 = iterator();
        CloseableIterator<Instance> iterator2 = other.iterator();
        try {
            while (iterator1.hasNext() && iterator2.hasNext()) {
                Instance instance1 = iterator1.next();
                Instance instance2 = iterator2.next();
                if (!instance1.equals(instance2)) {
                    return false;
                }
            }
            if (iterator1.hasNext() || iterator2.hasNext()) {
                throw new IllegalStateException("datasets were of different sizes, although their #size claimed they were equal.");
            }
        } finally {
            FileHelper.close(iterator1);
            FileHelper.close(iterator2);
        }
        return true;
    }

}
