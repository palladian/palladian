package ws.palladian.classification.discretization;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.classification.discretization.Binner.Interval;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.NoProgress;
import ws.palladian.helper.ProgressReporter;
import ws.palladian.helper.collection.Vector.VectorEntry;

public final class Discretization extends AbstractDatasetFeatureVectorTransformer {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Discretization.class);

    private final Map<String, Binner> binners = new HashMap<>();

    public Discretization(Dataset dataset) {
    	this(dataset, NoProgress.INSTANCE);
    }
    
    public Discretization(Dataset dataset, ProgressReporter progress) {
    	Validate.notNull(dataset, "dataset must not be null");
    	Validate.notNull(progress, "progress must not be null");
    	Set<String> numericFeatureNames = dataset.getFeatureInformation().getFeatureNamesOfType(NumericValue.class);
    	progress.startTask("Discretizing", numericFeatureNames.size());
    	for (String featureName : numericFeatureNames) {
    		LOGGER.debug("Discretizing {}", featureName);
    		binners.put(featureName, new Binner(dataset, featureName));
    		progress.increment();
    	}
    	progress.finishTask();
    }

    @Override
	public FeatureVector apply(FeatureVector featureVector) {
        Validate.notNull(featureVector, "featureVector must not be null");
        InstanceBuilder instanceBuilder = new InstanceBuilder();
        for (VectorEntry<String, Value> vectorEntry : featureVector) {
            String featureName = vectorEntry.key();
            Value featureValue = vectorEntry.value();
            if (featureValue instanceof NumericValue) {
                NumericValue numericValue = (NumericValue)featureValue;
                Binner binner = binners.get(featureName);
                Interval bin = binner.getBin(numericValue.getDouble());
                instanceBuilder.set(featureName, bin);
            } else {
                instanceBuilder.set(featureName, featureValue);
            }
        }
        return instanceBuilder.create();
    }

    public Binner getBinner(String featureName) {
        Validate.notEmpty(featureName, "featureName must not be empty");
        return binners.get(featureName);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (Binner binner : binners.values()) {
            builder.append(binner + "\n");
        }
        return builder.toString();
    }

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		FeatureInformationBuilder builder = new FeatureInformationBuilder();
		for (FeatureInformationEntry infoEntry : featureInformation) {
			if (infoEntry.isCompatible(NumericValue.class)) {
				builder.set(infoEntry.getName(), Interval.class);
			} else {
				builder.set(infoEntry);
			}
		}
		return builder.create();
	}

}
