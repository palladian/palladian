package ws.palladian.classification;

import java.util.List;

import ws.palladian.model.features.ClassificationFeatureVector;
import ws.palladian.processing.features.FeatureVector;

public interface Predictor<T> {
    
    void learn(List<Instance2<T>> instances);
    
    // FIXME <T> for CategoryEntries -> contract T in T out
    CategoryEntries predict(FeatureVector vector);
    
}