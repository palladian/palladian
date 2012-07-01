package ws.palladian.model.features;

import java.util.ArrayList;
import java.util.List;

import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureVector;

/**
 * <p>A feature vector used for classification tasks.</p>
 * @author David Urbansky
 */
public final class ClassificationFeatureVector extends FeatureVector {
    
    public List<Feature<String>> getNominalFeatures() {        
        return getAll(String.class);        
    }
    
    public List<String> getNominalFeatureValues() {        
        List<String> featureValues = new ArrayList<String>();
        
        for (Feature<String> feature : getNominalFeatures()) {
            featureValues.add(feature.getValue());
        }
        
        return featureValues;
    }
    
    public List<Feature<Double>> getNumericFeatures() {        
        return getAll(Double.class);        
    }
    
    public List<Double> getNumericFeatureValues() {        
        List<Double> featureValues = new ArrayList<Double>();
        
        for (Feature<Double> feature : getNumericFeatures()) {
            featureValues.add(feature.getValue());
        }
        
        return featureValues;
    }
}
