package ws.palladian.classification;

import java.io.Serializable;
import java.util.List;

import ws.palladian.model.features.ClassificationFeatureVector;
import ws.palladian.processing.features.FeatureVector;

//FIXME think of Serializable issue
public interface Predictor<T> extends Serializable {
    
    void learn(List<Instance2<T>> instances);
    
    // FIXME <T> for CategoryEntries -> contract T in T out
    CategoryEntries predict(FeatureVector vector);
    
}