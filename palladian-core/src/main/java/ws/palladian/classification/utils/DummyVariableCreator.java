package ws.palladian.classification.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.DatasetTransformer;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.value.ImmutableDoubleValue;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.nlp.StringPool;

/**
 * <p>
 * Convert {@link NominalFeature}s to {@link NumericFeature}s. For {@link NominalFeature}s with more than two values, a
 * new {@link NumericFeature} for each potential value is created. For {@link NominalFeature}s with two values, one new
 * {@link NumericFeature} is created. This way, {@link NominalFeature}s can be converted, so that they can be used for
 * regression e.g.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href="http://de.slideshare.net/jtneill/multiple-linear-regression/15">Dummy variables</a>
 * @see <a href="http://en.wikiversity.org/wiki/Dummy_variable_(statistics)">Dummy variable (statistics)</a>
 */
public class DummyVariableCreator implements Serializable, DatasetTransformer {

    private static final long serialVersionUID = 1L;

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyVariableCreator.class);

    // TODO for a nominal feature with k values, k-1 numeric features are enough

    private transient MultiMap<String, String> domain;

    private transient StringPool stringPool = new StringPool();

    /**
     * Create a new {@link DummyVariableCreator} for the given dataset.
     * @param dataset The dataset, not <code>null</code>.
     */
    public DummyVariableCreator(Dataset dataset) {
    	Validate.notNull(dataset, "dataset must not be null");
    	this.domain = determineDomains(dataset);
    }
	
    private static MultiMap<String, String> determineDomains(Dataset dataset) {
        MultiMap<String, String> domain = DefaultMultiMap.createWithList();
        Set<String> nominalFeatureNames = dataset.getFeatureInformation().getFeatureNamesOfType(NominalValue.class);
        if (nominalFeatureNames.isEmpty()) {
        	LOGGER.debug("No nominal features in dataset.");
        } else {
        	LOGGER.debug("Determine domain for dataset ...");
        	StopWatch stopWatch = new StopWatch();
	        for (Instance instance : dataset) {
	            for (String featureName : nominalFeatureNames) {
	                Value value = instance.getVector().get(featureName);
	                if (value == NullValue.NULL) {
	                    continue;
	                }
	                NominalValue nominalValue = (NominalValue)value;
	                String featureValue = nominalValue.getString();
	                if (!domain.get(featureName).contains(featureValue)) {
	                    domain.add(featureName, featureValue);
	                }
	            }
	        }
	        LOGGER.debug("... finished determining domain in {}", stopWatch);
        }
        return domain;
	}

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		FeatureInformationBuilder resultBuilder = new FeatureInformationBuilder();
		for (FeatureInformationEntry infoEntry : featureInformation) {
			Collection<String> featureDomain = domain.get(infoEntry.getName());
			if (featureDomain.isEmpty()) {
				resultBuilder.set(infoEntry.getName(), infoEntry.getType());
			} else if (featureDomain.size() < 3) {
				resultBuilder.set(infoEntry.getName(), ImmutableDoubleValue.class);
			} else {
				for (String domainValue : featureDomain) {
					resultBuilder.set(infoEntry.getName() + ":" + domainValue, ImmutableDoubleValue.class);
				}
			}
		}
		return resultBuilder.create();
	}
    
	@Override
	public Instance compute(final Instance input) {
		return new Instance() {
			@Override
			public int getWeight() {
				return input.getWeight();
			}
			@Override
			public FeatureVector getVector() {
				return convert(input.getVector()); // calculate lazily
			}
			@Override
			public String getCategory() {
				return input.getCategory();
			}
		};
	}

    /**
     * <p>
     * Convert the nominal values for the given {@link FeatureVector}.
     * </p>
     * 
     * @param featureVector The feature vector to convert, not <code>null</code>.
     * @return A feature vector with converted nominal features.
     */
    public FeatureVector convert(FeatureVector featureVector) {
        Validate.notNull(featureVector, "featureVector must not be null");
        InstanceBuilder builder = new InstanceBuilder();
        for (VectorEntry<String, Value> entry : featureVector) {
            String featureName = entry.key();
            Value featureValue = entry.value();
            if (featureValue instanceof NullValue) {
                Collection<String> featureDomain = domain.get(featureName);
                if (featureDomain.isEmpty()) {
                	builder.setNull(featureName);
                } else if (featureDomain.size() < 3) {
                    builder.set(featureName, 0);
                } else {
                    for (String domainValue : featureDomain) {
                        String newFeatureName = stringPool.get(featureName + ":" + domainValue);
                        builder.set(newFeatureName, 0);
                    }
                }
            } else if (featureValue instanceof NominalValue) {
                String nominalValue = ((NominalValue)featureValue).getString();
                Collection<String> featureDomain = domain.get(featureName);
                if (featureDomain.isEmpty()) {
                    builder.set(featureName, featureValue);
                } else if (featureDomain.size() < 3) {
                    double numericValue = nominalValue.equals(CollectionHelper.getFirst(featureDomain)) ? 1 : 0;
                    builder.set(featureName, numericValue);
                } else {
                    for (String domainValue : featureDomain) {
                        int numericValue = nominalValue.equals(domainValue) ? 1 : 0;
                        String newFeatureName = stringPool.get(featureName + ":" + domainValue);
                        builder.set(newFeatureName, numericValue);
                    }
                }
            } else {
                builder.set(featureName, featureValue);
            }
        }
        return builder.create();
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("NumericFeatureMapper\n");
        for (String entry : domain.keySet()) {
            toStringBuilder.append(entry).append(":").append(domain.get(entry)).append('\n');
        }
        toStringBuilder.append('\n');
        toStringBuilder.append("# nominal features: ").append(getNominalFeatureCount()).append('\n');
        toStringBuilder.append("# created numeric features: ").append(getCreatedNumericFeatureCount());
        return toStringBuilder.toString();
    }

    /** Package-private for testing purposes. */
    int getNominalFeatureCount() {
        return domain.keySet().size();
    }

    /** Package-private for testing purposes. */
    int getCreatedNumericFeatureCount() {
        int numCreatedNumericFeatures = 0;
        for (Collection<?> value : domain.values()) {
            numCreatedNumericFeatures += value.size() < 3 ? 1 : value.size();
        }
        return numCreatedNumericFeatures;
    }
    
    // custom serialization/deserialization code
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(getNominalFeatureCount()); // number of entries
        for (String featureName : domain.keySet()) {
            out.writeObject(featureName); // name of the current feature
            Collection<String> values = domain.get(featureName);
            out.writeInt(values.size()); // number of following entries
            for (String value : values) {
                out.writeObject(value);
            }
        }
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        domain = DefaultMultiMap.createWithList();
        stringPool = new StringPool(); // just create a new one
        int featureCount = in.readInt();
        for (int i = 0; i < featureCount; i++) {
            String featureName = (String)in.readObject();
            int entryCount = in.readInt();
            for (int j = 0; j < entryCount; j++) {
                String value = (String)in.readObject();
                domain.add(featureName, value);
            }
        }
    }

}
