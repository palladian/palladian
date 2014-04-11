package ws.palladian.classification.discretization;

import java.util.Map;
import java.util.Set;

import ws.palladian.classification.utils.AbstractNormalization;
import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.Normalization;
import ws.palladian.classification.utils.Normalizer;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.CollectionHelper;

public class Discretizer implements Normalizer {

    private static final class Discretization extends AbstractNormalization {

        private static final long serialVersionUID = 1L;
        
        private final Map<String, Binner> binners;

        Discretization(Map<String, Binner> binners) {
            this.binners = binners;
        }

        @Override
        public double normalize(String name, double value) {
            return binners.get(name).bin(value);
        }

    }

    @Override
    public Normalization calculate(Iterable<? extends FeatureVector> featureVectors) {
        Map<String, Binner> binners = CollectionHelper.newHashMap();
        Set<String> featureNames = ClassificationUtils.getFeatureNames(featureVectors);
        FeatureVector firstFeatureVector = featureVectors.iterator().next();
        for (String featureName : featureNames) {
            Value value = firstFeatureVector.get(featureName);
            if (value instanceof NumericValue) {
                // create binner
            }
        }
        return new Discretization(binners);
    }

}
