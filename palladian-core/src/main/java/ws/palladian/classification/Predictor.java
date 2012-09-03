package ws.palladian.classification;

import java.util.List;

import ws.palladian.processing.features.FeatureVector;

//FIXME think of Serializable issue
public interface Predictor<M extends Model> {
    
    Model learn(List<NominalInstance> instances);
    
    // FIXME <T> for CategoryEntries -> contract T in T out
    CategoryEntries predict(FeatureVector vector, M model);
    
}