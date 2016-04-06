package ws.palladian.classification.utils;

import java.util.Iterator;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.ImmutableInstance;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDataset;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.AbstractIterator;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.io.CloseableIterator;
import ws.palladian.helper.io.CloseableIteratorAdapter;

/**
 * <p>
 * Abstract {@link Normalization} functionality.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractNormalization implements Normalization {

    @Override
    public FeatureVector normalize(FeatureVector featureVector) {
        Validate.notNull(featureVector, "featureVector must not be null");
        InstanceBuilder builder = new InstanceBuilder();
        for (VectorEntry<String, Value> entry : featureVector) {
            String name = entry.key();
            Value value = entry.value();
            if (value instanceof NumericValue) {
                double normalizedValue = normalize(name, ((NumericValue)value).getDouble());
                builder.set(name, normalizedValue);
            } else {
                builder.set(name, value);
            }
        }
        return builder.create();
    }
    
    @Override
    public Dataset normalize(final Dataset dataset) {
    	Validate.notNull(dataset, "dataset must not be null");
    	
    	// XXX partly copied from ws.palladian.classification.utils.DummyVariableCreator.convert(Dataset)
    	return new AbstractDataset() {
			
			@Override
			public long size() {
				return dataset.size();
			}
			
			@Override
			public CloseableIterator<Instance> iterator() {
				final CloseableIterator<Instance> wrapped = dataset.iterator();
				
				Iterator<Instance> iterator = new AbstractIterator<Instance>() {

					@Override
					protected Instance getNext() throws Finished {
						if (wrapped.hasNext()) {
							Instance current = wrapped.next();
							return new ImmutableInstance(normalize(current.getVector()), current.getCategory());
						}
						throw FINISHED;
					}

				};
				
				// FIXME iterator should actually be closed
				return new CloseableIteratorAdapter<>(iterator);
			}
			
			@Override
			public FeatureInformation getFeatureInformation() {
				return dataset.getFeatureInformation();
			}
		};
    }

}
