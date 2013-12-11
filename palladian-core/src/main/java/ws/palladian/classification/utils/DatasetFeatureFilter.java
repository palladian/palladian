package ws.palladian.classification.utils;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Instance;
import ws.palladian.helper.collection.Filter;
import ws.palladian.processing.Trainable;

/**
 * <p>
 * Filter features from a dataset, represented by an {@link Iterable} of {@link Trainable}s.
 * </p>
 * 
 * @author pk
 */
final class DatasetFeatureFilter implements Iterable<Trainable> {

    private final Iterable<? extends Trainable> dataset;
    private final Filter<? super String> nameFilter;

    /**
     * @param dataset The {@link Iterable} dataset to filter, not <code>null</code>.
     * @param nameFilter The {@link Filter} for the feature's names to apply, not <code>null</code>.
     */
    public DatasetFeatureFilter(Iterable<? extends Trainable> dataset, Filter<? super String> nameFilter) {
        Validate.notNull(dataset, "dataset must not be null");
        Validate.notNull(nameFilter, "filter must not be null");
        this.dataset = dataset;
        this.nameFilter = nameFilter;
    }

    @Override
    public Iterator<Trainable> iterator() {
        final Iterator<? extends Trainable> iterator = dataset.iterator();
        return new Iterator<Trainable>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Trainable next() {
                Trainable item = iterator.next();
                return new Instance(item.getTargetClass(), ClassificationUtils.filterFeatures(item, nameFilter));
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
