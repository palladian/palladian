package ws.palladian.classification.utils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.DefaultMultiMap;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.Trainable;
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
 */
public class DummyVariableCreator implements Iterable<Trainable> {

    // TODO for a nominal feature with k values, k-1 numeric features are enough

    private final Iterable<Trainable> wrapped;

    private final MultiMap<String, String> domain;

    public DummyVariableCreator(Iterable<Trainable> wrapped) {
        Validate.notNull(wrapped, "wrapped must not be null");
        this.wrapped = wrapped;
        this.domain = determineDomains(wrapped);
    }

    private static MultiMap<String, String> determineDomains(Iterable<Trainable> dataset) {
        MultiMap<String, String> domain = DefaultMultiMap.createWithList();
        Set<String> nominalFeatureNames = null;
        for (Trainable trainable : dataset) {
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

    private static Set<String> getNominalFeatureNames(Trainable trainable) {
        Collection<NominalFeature> nominalFeatures = trainable.getFeatureVector().getAll(NominalFeature.class);
        Set<String> nominalFeatureNames = CollectionHelper.newHashSet();
        for (NominalFeature nominalFeature : nominalFeatures) {
            nominalFeatureNames.add(nominalFeature.getName());
        }
        return nominalFeatureNames;
    }

    @Override
    public Iterator<Trainable> iterator() {
        final Iterator<Trainable> iterator = wrapped.iterator();

        return new Iterator<Trainable>() {

            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public Trainable next() {
                return convert(iterator.next());
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    private Trainable convert(Trainable trainable) {
        FeatureVector convertedFeatureVector = new BasicFeatureVector();
        for (Feature<?> feature : trainable.getFeatureVector()) {
            if (feature instanceof NominalFeature) {
                NominalFeature nominalFeature = (NominalFeature)feature;
                String nominalValue = nominalFeature.getValue();
                Collection<String> featureDomain = domain.get(feature.getName());
                if (featureDomain.size() < 3) {
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
        return new Instance(trainable.getTargetClass(), convertedFeatureVector);
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append("NumericFeatureMapper\n");
        int numCreatedNumericFeatures = 0;
        for (String entry : domain.keySet()) {
            toStringBuilder.append(entry).append(":").append(domain.get(entry)).append('\n');
            numCreatedNumericFeatures += domain.get(entry).size() < 3 ? 1 : domain.get(entry).size();
        }
        toStringBuilder.append('\n');
        toStringBuilder.append("# nominal features: ").append(domain.keySet().size()).append('\n');
        toStringBuilder.append("# created numeric features: ").append(numCreatedNumericFeatures);
        return toStringBuilder.toString();
    }

}
