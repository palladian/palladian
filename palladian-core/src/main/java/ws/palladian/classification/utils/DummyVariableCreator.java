package ws.palladian.classification.utils;

import java.io.Serializable;
import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.Classifiable;
import ws.palladian.processing.features.BasicFeatureVector;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

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
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DummyVariableCreator.class);

    private static final long serialVersionUID = 1L;

    // TODO for a nominal feature with k values, k-1 numeric features are enough

    private final MultiMap<String, String> domain;

    /**
     * <p>
     * Create a new {@link DummyVariableCreator} for the given dataset.
     * </p>
     * 
     * @param trainingSet The dataset, not <code>null</code>.
     */
    public DummyVariableCreator(Iterable<? extends Classifiable> trainingSet) {
        Validate.notNull(trainingSet, "wrapped must not be null");
        this.domain = determineDomains(trainingSet);
    }

    private static MultiMap<String, String> determineDomains(Iterable<? extends Classifiable> dataset) {
        MultiMap<String, String> domain = DefaultMultiMap.createWithList();
        Set<String> nominalFeatureNames = null;
        for (Classifiable trainable : dataset) {
            if (nominalFeatureNames == null) {
                nominalFeatureNames = getNominalFeatureNames(trainable);
            }
            for (String featureName : nominalFeatureNames) {
                NominalFeature feature = trainable.getFeatureVector().get(NominalFeature.class, featureName);
                if (feature == null) {
                    continue;
                }
                String featureValue = feature.getValue();
                if (!domain.get(featureName).contains(featureValue)) {
                    domain.add(featureName, featureValue);
                }
            }
        }
        return domain;
    }

    private static Set<String> getNominalFeatureNames(Classifiable trainable) {
        Collection<NominalFeature> nominalFeatures = trainable.getFeatureVector().getAll(NominalFeature.class);
        Set<String> nominalFeatureNames = CollectionHelper.newHashSet();
        for (NominalFeature nominalFeature : nominalFeatures) {
            nominalFeatureNames.add(nominalFeature.getName());
        }
        return nominalFeatureNames;
    }

    /**
     * <p>
     * Convert the {@link NominalFeature}s for the given {@link Iterable} dataset.
     * </p>
     * 
     * @param data The dataset, not <code>null</code>.
     * @return Dataset with converted features.
     */
    public Iterable<Classifiable> convert(final Iterable<? extends Classifiable> data) {
        Validate.notNull(data, "data must not be null");

        return new Iterable<Classifiable>() {

            @Override
            public Iterator<Classifiable> iterator() {
                return new Iterator<Classifiable>() {

                    Iterator<? extends Classifiable> wrapped = data.iterator();

                    @Override
                    public boolean hasNext() {
                        return wrapped.hasNext();
                    }

                    @Override
                    public Classifiable next() {
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
     * Convert the {@link NominalFeature}s for the given {@link Classifiable}.
     * </p>
     * 
     * @param classifiable The classifiable to convert, not <code>null</code>.
     * @return A classifiable with converted nominal features.
     */
    public Classifiable convert(Classifiable classifiable) {
        Validate.notNull(classifiable, "trainable must not be null");
        FeatureVector convertedFeatureVector = new BasicFeatureVector();
        for (Feature<?> feature : classifiable.getFeatureVector()) {
            if (feature instanceof NominalFeature) {
                NominalFeature nominalFeature = (NominalFeature)feature;
                String nominalValue = nominalFeature.getValue();
                Collection<String> featureDomain = domain.get(feature.getName());
                if (featureDomain.isEmpty()) {
                    LOGGER.debug("Unknown feature {} will be dropped", feature.getName());
                } else if (featureDomain.size() < 3) {
                    double numericValue = nominalValue.equals(CollectionHelper.getFirst(featureDomain)) ? 1 : 0;
                    convertedFeatureVector.add(new NumericFeature(feature.getName(), numericValue));
                } else {
                    for (String domainValue : featureDomain) {
                        double numericValue = nominalValue.equals(domainValue) ? 1 : 0;
                        String featureName = feature.getName() + ":" + domainValue;
                        convertedFeatureVector.add(new NumericFeature(featureName, numericValue));
                    }
                }
            } else {
                convertedFeatureVector.add(feature);
            }
        }
        return convertedFeatureVector;
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

}
