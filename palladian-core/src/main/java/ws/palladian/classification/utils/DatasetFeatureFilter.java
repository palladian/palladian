package ws.palladian.classification.utils;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.helper.functional.Filter;

/**
 * <p>
 * Filter features from a dataset, represented by an {@link Iterable} of {@link Instance}s.
 * </p>
 * 
 * @author pk
 */
final class DatasetFeatureFilter implements Iterable<Instance> {

    private final Iterable<? extends Instance> dataset;
    private final Filter<? super String> nameFilter;

    /**
     * @param dataset The {@link Iterable} dataset to filter, not <code>null</code>.
     * @param nameFilter The {@link Filter} for the feature's names to apply, not <code>null</code>.
     */
    public DatasetFeatureFilter(Iterable<? extends Instance> dataset, Filter<? super String> nameFilter) {
        Validate.notNull(dataset, "dataset must not be null");
        Validate.notNull(nameFilter, "filter must not be null");
        this.dataset = dataset;
        this.nameFilter = nameFilter;
    }

    @Override
    public Iterator<Instance> iterator() {
        final Iterator<? extends Instance> iterator = dataset.iterator();
        return new Iterator<Instance>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Instance next() {
                Instance item = iterator.next();
                FeatureVector filteredFeatures = ClassificationUtils.filterFeatures(item.getVector(), nameFilter);
                return new InstanceBuilder().add(filteredFeatures).create(item.getCategory());
            }

            @Override
            public void remove() {
                iterator.remove();
            }
        };
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("DatasetFeatureFilter [nameFilter=");
        builder.append(nameFilter);
        builder.append("]");
        return builder.toString();
    }

}
