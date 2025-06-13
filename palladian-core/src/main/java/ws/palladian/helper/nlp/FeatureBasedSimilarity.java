package ws.palladian.helper.nlp;

import org.apache.commons.lang3.Validate;
import ws.palladian.classification.text.FeatureSetting;
import ws.palladian.classification.text.Preprocessor;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.math.SetSimilarity;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public final class FeatureBasedSimilarity extends AbstractStringMetric {
    private final FeatureSetting featureSetting;

    private final SetSimilarity setSimilarity;

    private final Preprocessor preprocessor;

    public FeatureBasedSimilarity(FeatureSetting featureSetting, SetSimilarity setSimilarity) {
        Validate.notNull(featureSetting, "featureSetting must not be null");
        Validate.notNull(setSimilarity, "setSimilarity must not be null");
        this.featureSetting = featureSetting;
        this.setSimilarity = setSimilarity;
        this.preprocessor = new Preprocessor(featureSetting);
    }

    @Override
    public double getSimilarity(String s1, String s2) {
        Validate.notNull(s1, "s1 must not be null");
        Validate.notNull(s2, "s2 must not be null");
        if (s1.equals(s2)) {
            return 1;
        }
        Set<String> features1 = extractFeatures(s1);
        Set<String> features2 = extractFeatures(s2);
        return setSimilarity.getSimilarity(features1, features2);
    }

    private Set<String> extractFeatures(String s) {
        Iterator<String> featureIterator = preprocessor.apply(s);
        return CollectionHelper.newHashSet(featureIterator);
    }

    @Override
    public String toString() {
        return new StringBuilder().append(featureSetting).append('-').append(setSimilarity).toString();
    }

}
