package ws.palladian.model.features;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.classification.numeric.NumericClassifier;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>A feature vector used for classification tasks.</p>
 * @author David Urbansky
 */
public final class ClassificationFeatureVector extends FeatureVector {
    
    public List<NominalFeature> getNominalFeatures() {        
        return getAll(NominalFeature.class);        
    }
    
    public List<String> getNominalFeatureValues() {        
        List<String> featureValues = new ArrayList<String>();
        
        for (Feature<String> feature : getNominalFeatures()) {
            featureValues.add(feature.getValue());
        }
        
        return featureValues;
    }
    
    public List<NumericFeature> getNumericFeatures() {        
        return getAll(NumericFeature.class);        
    }
    
    public List<Double> getNumericFeatureValues() {        
        List<Double> featureValues = new ArrayList<Double>();
        
        for (Feature<Double> feature : getNumericFeatures()) {
            featureValues.add(feature.getValue());
        }
        
        return featureValues;
    }
}
