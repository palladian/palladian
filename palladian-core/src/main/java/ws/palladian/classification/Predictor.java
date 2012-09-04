package ws.palladian.classification;

import java.util.List;

import ws.palladian.processing.features.FeatureVector;

public interface Predictor<M extends Model> {
    
    M learn(List<NominalInstance> instances);
    
    CategoryEntries predict(FeatureVector vector, M model);
    
}