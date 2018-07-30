package ws.palladian.classification.utils;

import static ws.palladian.helper.functional.Filters.equal;
import static ws.palladian.helper.functional.Filters.not;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.AppendedVector;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.FilteredVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.dataset.AbstractDatasetFeatureVectorTransformer;
import ws.palladian.core.dataset.Dataset;
import ws.palladian.core.dataset.FeatureInformation;
import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.dataset.FeatureInformationBuilder;
import ws.palladian.core.dataset.statistics.DatasetStatistics;
import ws.palladian.core.dataset.statistics.NominalValueStatistics;
import ws.palladian.core.featurevector.FlyweightVectorBuilder;
import ws.palladian.core.featurevector.FlyweightVectorSchema;
import ws.palladian.core.value.ImmutableIntegerValue;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.Vector.VectorEntry;

/**
 * <p>
 * Convert {@link NominalFeature}s to {@link NumericFeature}s by applying
 * "one-hot" encoding. For {@link NominalFeature}s with more than two values, a
 * new {@link NumericFeature} for each potential value is created. For
 * {@link NominalFeature}s with two values, one new {@link NumericFeature} is
 * created. This way, {@link NominalFeature}s can be converted, so that they can
 * be used for regression or classifiers which only support numerical input e.g.
 * </p>
 * 
 * @author Philipp Katz
 * @see <a href=
 *      "http://de.slideshare.net/jtneill/multiple-linear-regression/15">Dummy
 *      variables</a>
 * @see <a href=
 *      "http://en.wikiversity.org/wiki/Dummy_variable_(statistics)">Dummy
 *      variable (statistics)</a>
 */
public class DummyVariableCreator extends AbstractDatasetFeatureVectorTransformer implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyVariableCreator.class);
    
	private static final class Mapper {
		final Map<String, FeatureVector> mapping = new HashMap<>();
		final FeatureVector missing;
		final FeatureInformation featureInformation;
		Mapper(String featureName, NominalValueStatistics stats, boolean dense) {
			this(prepare(featureName, stats), dense);
		}
		Mapper(Map<String, String> mapping, boolean dense) {
			FlyweightVectorSchema schema = null;
			if (dense) {
				schema = new FlyweightVectorSchema(mapping.values().toArray(new String[0]));
			}
			for (Entry<String, String> entry : mapping.entrySet()) {
				this.mapping.put(entry.getKey(), createDummyVector(schema, entry.getValue(), mapping.values(), dense));
			}
			missing = createDummyVector(schema, null, mapping.values(), dense);
			featureInformation = new FeatureInformationBuilder().set(mapping.values(), ImmutableIntegerValue.class)
					.create();
		}
		private static Map<String, String> prepare(String featureName, NominalValueStatistics stats) {
			Set<String> values = stats.getValues();
			if (stats.getNumUniqueValuesIncludingNull() <= 2) {
				// in case, we have two values and no null values we can map to
				// only one column which denotes presence of one of the values,
				// e.g. true
				if (values.containsAll(Arrays.asList("true", "false"))) {
					values = Collections.singleton("true");
				} else if (values.size() > 0) {
					values = Collections.singleton(values.iterator().next());
				}
			}
			Map<String, String> prefixedValues = new HashMap<>();
			for (String value : values) {
				prefixedValues.put(value, featureName + ":" + value);
			}
			return prefixedValues;
		}
		private static FeatureVector createDummyVector(FlyweightVectorSchema schema, String value,
				Collection<String> allValues, boolean dense) {
			if (dense) {
				FlyweightVectorBuilder builder = schema.builder();
				for (String currentValue : allValues) {
					int mappedValue = currentValue.equals(value) ? 1 : 0;
					builder.set(currentValue, ImmutableIntegerValue.valueOf(mappedValue));
				}
				return builder.create();
			} else /* sparse */ {
				InstanceBuilder builder = new InstanceBuilder();
				if (value != null) {
					builder.set(value, 1);
				}
				return builder.create();
			}
		}
		public FeatureInformation getFeatureInformation() {
			return featureInformation;
		}
		public FeatureVector getAppendedFeatureVector(Value value) {
			FeatureVector result = mapping.get(value.toString());
			return result != null ? result : missing;
		}
		@Override
		public String toString() {
			return mapping.toString();
		}
	}

    private transient Map<String, Mapper> mappers;

	private transient boolean keepOriginalFeature = false;

	private transient boolean dense = true;

    /**
     * Create a new {@link DummyVariableCreator} for the given dataset.
     * @param dataset The dataset, not <code>null</code>.
	 * @deprecated Prefer using the
	 *             {@link #DummyVariableCreator(Dataset, boolean, boolean)}
	 *             constructor and set <code>dense</code> to <code>false</code>
	 *             for better performance and memory-efficiency.
     */
	@Deprecated
    public DummyVariableCreator(Dataset dataset) {
    	this(dataset, false);
    }
    
	/**
	 * @deprecated Prefer using the
	 *             {@link #DummyVariableCreator(Dataset, boolean, boolean)}
	 *             constructor and set <code>dense</code> to <code>false</code>
	 *             for better performance and memory-efficiency.
	 */
    @Deprecated
    public DummyVariableCreator(Dataset dataset, boolean keepOriginalFeature) {
    	this(dataset, keepOriginalFeature, true);
    }

	/**
	 * Create a new {@link DummyVariableCreator} for the given dataset.
	 * 
	 * @param dataset
	 *            The dataset, not <code>null</code>.
	 * @param keepOriginalFeature
	 *            <code>true</code> in order to keep the original nominal
	 *            feature, <code>false</code> to remove it (default setting).
	 * @param dense
	 *            <code>true</code> to create zero-values explicitly,
	 *            <code>false</code> in order to simply keep them unset, which
	 *            is fine in most cases and saves time and memory.
	 */
	public DummyVariableCreator(Dataset dataset, boolean keepOriginalFeature, boolean dense) {
		Validate.notNull(dataset, "dataset must not be null");
		mappers = buildMappers(dataset, dense);
		if (getNominalFeatureCount() > 0) {
			LOGGER.info("# nominal features which will be mapped: {}", getNominalFeatureCount());
			LOGGER.info("# created features: {}", getCreatedNumericFeatures().size());
		}
		this.keepOriginalFeature = keepOriginalFeature;
		this.dense = dense;
	}

	private static Map<String, Mapper> buildMappers(Dataset dataset, boolean dense) {
		Map<String, Mapper> mappers = new HashMap<>();
		Set<String> nominalFeatureNames = dataset.getFeatureInformation().getFeatureNamesOfType(NominalValue.class);
		if (nominalFeatureNames.isEmpty()) {
			LOGGER.debug("No nominal features in dataset.");
		} else {

			LOGGER.debug("Determine domain for dataset ...");
			StopWatch stopWatch = new StopWatch();

			Dataset filteredDataset = dataset.filterFeatures(equal(nominalFeatureNames));
			DatasetStatistics statistics = new DatasetStatistics(filteredDataset);
			for (String featureName : nominalFeatureNames) {
				NominalValueStatistics valueStats = (NominalValueStatistics) statistics.getValueStatistics(featureName);
				mappers.put(featureName, new Mapper(featureName, valueStats, dense));
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
	public FeatureVector apply(FeatureVector featureVector) {
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
        
		if (mappers.isEmpty()) {
			return featureVector;
		}
        
        List<FeatureVector> appendedVectors = new ArrayList<>();

        if (keepOriginalFeature) {
        	appendedVectors.add(featureVector);
        } else {
        	appendedVectors.add(new FilteredVector(featureVector, not(equal(mappers.keySet()))));
        }
        
        for (VectorEntry<String, Value> entry : featureVector) {
            String featureName = entry.key();
            Value featureValue = entry.value();
            Mapper mapper = mappers.get(featureName);
            if (mapper != null) {
            	appendedVectors.add(mapper.getAppendedFeatureVector(featureValue));
            }
        }
        
		return new AppendedVector(appendedVectors);
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
        toStringBuilder.append("# created numeric features: ").append(getCreatedNumericFeatures().size());
        return toStringBuilder.toString();
    }

    /** Package-private for testing purposes. */
    final int getNominalFeatureCount() {
        return mappers.size();
    }

    /** Package-private for testing purposes. */
    Set<String> getCreatedNumericFeatures() {
    	Set<String> createdNominalFeatures = new HashSet<>();
    	for (Mapper mapper : mappers.values()) {
    		createdNominalFeatures.addAll(mapper.getFeatureInformation().getFeatureNames());
    	}
    	return createdNominalFeatures;
    }
    
    // custom serialization/deserialization code
    
    private void writeObject(ObjectOutputStream out) throws IOException {
        out.writeInt(getNominalFeatureCount()); // number of entries
        for (String featureName : mappers.keySet()) {
            out.writeObject(featureName); // name of the current feature
            Map<String, FeatureVector> values = mappers.get(featureName).mapping;
            out.writeInt(values.size()); // number of following entries
            for (Entry<String, FeatureVector> entry : values.entrySet()) {
                out.writeObject(entry.getKey()); // original feature value, e.g. A
                for (VectorEntry<String, Value> vectorEntry : entry.getValue()) {
                	if (vectorEntry.value().equals(ImmutableIntegerValue.valueOf(1))) {
                		out.writeObject(vectorEntry.key()); // mapped feature name, e.g. featureX:A
                		break;
                	}
                }
            }
        }
        out.writeBoolean(keepOriginalFeature);
        out.writeBoolean(dense);
    }
    
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        Map<String, Map<String, String>> temp = new HashMap<>();
        int featureCount = in.readInt();
        for (int i = 0; i < featureCount; i++) {
            String featureName = (String)in.readObject();
            Map<String, String> mapping = new HashMap<>();
            int entryCount = in.readInt();
            for (int j = 0; j < entryCount; j++) {
                String key = (String)in.readObject(); // original feature value, e.g. A
                String value = (String)in.readObject(); // mapped feature name, e.g. featureX:A
                mapping.put(key, value);
            }
            temp.put(featureName, mapping);
        }
        try {
			keepOriginalFeature = in.readBoolean();
			dense = in.readBoolean();
		} catch (EOFException ignore) {
		}
        mappers = new HashMap<>();
        for (Entry<String, Map<String, String>> entry : temp.entrySet()) {
        	mappers.put(entry.getKey(), new Mapper(entry.getValue(), dense));
        }
    }
    

}
