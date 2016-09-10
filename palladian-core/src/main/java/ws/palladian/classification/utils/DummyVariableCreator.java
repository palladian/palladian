package ws.palladian.classification.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.dataset.statistics.DatasetStatistics;
import ws.palladian.core.dataset.statistics.NominalValueStatistics;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.functional.Filters;

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
public class DummyVariableCreator extends AbstractDatasetFeatureVectorTransformer implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyVariableCreator.class);
    
	private static final class Mapper {
		final Map<String, String> mapping;
		Mapper(String featureName, NominalValueStatistics stats) {
			mapping = new HashMap<>();
			if (stats.getNumUniqueValuesIncludingNull() <= 2) {
				// in case, we have two values and no null values we can map to
				// only one column which denotes presence of one of the values,
				// e.g. true
				Set<String> values = stats.getValues();
				String value;
				if (values.containsAll(Arrays.asList("true", "false"))) {
					value = "true";
				} else {
					value = values.iterator().next();
				}
				String mappedValue = featureName + ":" + value;
				// if we have true/false, we take the "true" column without
				// suffix
				mapping.put(value, mappedValue);
			} else {
				for (String value : stats.getValues()) {
					mapping.put(value, featureName + ":" + value);
				}
			}
		}
		Mapper(Map<String, String> mapping) {
			this.mapping = mapping;
		}
		public FeatureInformation getFeatureInformation() {
			return new FeatureInformationBuilder().set(mapping.values(), ImmutableIntegerValue.class).create();
		}
		public void setValues(Value value, InstanceBuilder builder) {
			String nominalValue = null;
			if (value != NullValue.NULL) {
				nominalValue = ((NominalValue) value).getString();
			}
			for (Entry<String, String> currentMapping : mapping.entrySet()) {
				int numericValue = currentMapping.getKey().equals(nominalValue) ? 1 : 0;
				builder.set(currentMapping.getValue(), numericValue);
			}
		}
		@Override
		public String toString() {
			return mapping.toString();
		}
	}

    private transient Map<String, Mapper> mappers;

	private final boolean keepOriginalFeature;

    /**
     * Create a new {@link DummyVariableCreator} for the given dataset.
     * @param dataset The dataset, not <code>null</code>.
     */
    public DummyVariableCreator(Dataset dataset) {
    	this(dataset, false);
    }

	/**
	 * Create a new {@link DummyVariableCreator} for the given dataset.
	 * 
	 * @param dataset
	 *            The dataset, not <code>null</code>.
	 * @param keepOriginalFeature
	 *            <code>true</code> in order to keep the original nominal
	 *            feature, <code>false</code> to remove it (default setting).
	 */
	public DummyVariableCreator(Dataset dataset, boolean keepOriginalFeature) {
		Validate.notNull(dataset, "dataset must not be null");
		this.mappers = buildMappers(dataset);
		this.keepOriginalFeature = keepOriginalFeature;
    }
	
    private static Map<String, Mapper> buildMappers(Dataset dataset) {
        Map<String, Mapper> mappers = new HashMap<>();
        Set<String> nominalFeatureNames = dataset.getFeatureInformation().getFeatureNamesOfType(NominalValue.class);
        if (nominalFeatureNames.isEmpty()) {
        	LOGGER.debug("No nominal features in dataset.");
        } else {
        	
        	LOGGER.debug("Determine domain for dataset ...");
        	StopWatch stopWatch = new StopWatch();

			Dataset filteredDataset = dataset.filterFeatures(Filters.equal(nominalFeatureNames));
			DatasetStatistics statistics = new DatasetStatistics(filteredDataset);
			for (String featureName : nominalFeatureNames) {
				NominalValueStatistics valueStats = (NominalValueStatistics) statistics.getValueStatistics(featureName);
				mappers.put(featureName, new Mapper(featureName, valueStats));
			}
	        LOGGER.debug("... finished determining domain in {}", stopWatch);
        }
        return mappers;
	}

	@Override
	public FeatureInformation getFeatureInformation(FeatureInformation featureInformation) {
		FeatureInformationBuilder resultBuilder = new FeatureInformationBuilder();
		for (FeatureInformationEntry infoEntry : featureInformation) {
			Mapper mapper = mappers.get(infoEntry.getName());
			if (mapper == null || keepOriginalFeature) {
				resultBuilder.set(infoEntry);
			}
			if (mapper != null) {
				resultBuilder.add(mapper.getFeatureInformation());
			}
		}
		return resultBuilder.create();
	}
	
	@Override
	protected FeatureVector compute(FeatureVector featureVector) {
		return convert(featureVector);
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
            Mapper mapper = mappers.get(featureName);
            if (mapper == null || keepOriginalFeature) {
            	builder.set(featureName, featureValue);
            }
            if (mapper != null) {
            	mapper.setValues(featureValue, builder);
            }
        }
        return builder.create();
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("NumericFeatureMapper\n");
        for (Entry<String, Mapper> entry : mappers.entrySet()) {
            toStringBuilder.append(entry.getKey()).append(":").append(entry.getValue()).append('\n');
        }
        toStringBuilder.append('\n');
        toStringBuilder.append("# nominal features: ").append(getNominalFeatureCount()).append('\n');
        toStringBuilder.append("# created numeric features: ").append(getCreatedNumericFeatureCount());
        return toStringBuilder.toString();
    }

    /** Package-private for testing purposes. */
    int getNominalFeatureCount() {
        return mappers.size();
    }

    /** Package-private for testing purposes. */
    int getCreatedNumericFeatureCount() {
        int numCreatedNumericFeatures = 0;
        for (Mapper mapper : mappers.values()) {
            numCreatedNumericFeatures += mapper.getFeatureInformation().count();
        }
        return numCreatedNumericFeatures;
    }
    
    // custom serialization/deserialization code
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(getNominalFeatureCount()); // number of entries
        for (String featureName : mappers.keySet()) {
            out.writeObject(featureName); // name of the current feature
            Map<String, String> values = mappers.get(featureName).mapping;
            out.writeInt(values.size()); // number of following entries
            for (Entry<String, String> entry : values.entrySet()) {
                out.writeObject(entry.getKey());
                out.writeObject(entry.getValue());
            }
        }
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        mappers = new HashMap<>();
        int featureCount = in.readInt();
        for (int i = 0; i < featureCount; i++) {
            String featureName = (String)in.readObject();
            Map<String, String> mapping = new HashMap<>();
            int entryCount = in.readInt();
            for (int j = 0; j < entryCount; j++) {
                String key = (String)in.readObject();
                String value = (String)in.readObject();
                mapping.put(key, value);
            }
            mappers.put(featureName, new Mapper(mapping));
        }
    }

}
