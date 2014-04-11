package ws.palladian.classification.utils;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.NominalValue;
import ws.palladian.core.Value;
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
 * @author pk
 * @see <a href="http://de.slideshare.net/jtneill/multiple-linear-regression/15">Dummy variables</a>
 * @see <a href="http://en.wikiversity.org/wiki/Dummy_variable_(statistics)">Dummy variable (statistics)</a>
 */
public class DummyVariableCreator implements Serializable {

    private static final long serialVersionUID = 1L;

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyVariableCreator.class);

    // TODO for a nominal feature with k values, k-1 numeric features are enough

    private transient MultiMap<String, String> domain;

    private transient StringPool stringPool = new StringPool();

    /**
     * <p>
     * Create a new {@link DummyVariableCreator} for the given dataset.
     * </p>
     * 
     * @param trainingSet The dataset, not <code>null</code>.
     */
    public DummyVariableCreator(Iterable<? extends FeatureVector> dataSet) {
        Validate.notNull(dataSet, "dataSet must not be null");
        this.domain = determineDomains(dataSet);
    }

    private static MultiMap<String, String> determineDomains(Iterable<? extends FeatureVector> dataset) {
        LOGGER.debug("Determine domain for dataset ...");
        StopWatch stopWatch = new StopWatch();
        MultiMap<String, String> domain = DefaultMultiMap.createWithList();
        Set<String> nominalFeatureNames = null;
        for (FeatureVector featureVector : dataset) {
            if (nominalFeatureNames == null) {
                nominalFeatureNames = getNominalFeatureNames(featureVector);
                if (nominalFeatureNames.isEmpty()) {
                    LOGGER.debug("No nominal features in dataset.");
                    break;
                }
            }
            for (String featureName : nominalFeatureNames) {
                NominalValue value = (NominalValue)featureVector.get(featureName);
                if (value == null) {
                    continue;
                }
                String featureValue = value.getString();
                if (!domain.get(featureName).contains(featureValue)) {
                    domain.add(featureName, featureValue);
                }
            }
        }
        LOGGER.debug("... finished determining domain in {}", stopWatch);
        return domain;
    }

    private static Set<String> getNominalFeatureNames(FeatureVector featureVector) {
        Set<String> nominalFeatureNames = CollectionHelper.newHashSet();
        for (VectorEntry<String, Value> entry : featureVector) {
            if (entry.value() instanceof NominalValue) {
                nominalFeatureNames.add(entry.key());
            }
        }
        return nominalFeatureNames;
    }

    /**
     * <p>
     * Convert the nominal values for the given {@link Iterable} dataset.
     * </p>
     * 
     * @param data The dataset, not <code>null</code>.
     * @return Dataset with converted features.
     */
    public Iterable<FeatureVector> convert(final Iterable<? extends FeatureVector> data) {
        Validate.notNull(data, "data must not be null");

        return new Iterable<FeatureVector>() {

            @Override
            public Iterator<FeatureVector> iterator() {
                return new Iterator<FeatureVector>() {

                    Iterator<? extends FeatureVector> wrapped = data.iterator();

                    @Override
                    public boolean hasNext() {
                        return wrapped.hasNext();
                    }

                    @Override
                    public FeatureVector next() {
                        return convert(wrapped.next());
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Modifications are not allowed.");
                    }
                };
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
            if (featureValue instanceof NominalValue) {
                String nominalValue = ((NominalValue)featureValue).getString();
                Collection<String> featureDomain = domain.get(featureName);
                if (featureDomain.isEmpty()) {
                    LOGGER.debug("Unknown feature {} will be dropped", featureName);
                } else if (featureDomain.size() < 3) {
                    double numericValue = nominalValue.equals(CollectionHelper.getFirst(featureDomain)) ? 1 : 0;
                    builder.set(featureName, numericValue);
                } else {
                    for (String domainValue : featureDomain) {
                        double numericValue = nominalValue.equals(domainValue) ? 1 : 0;
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
