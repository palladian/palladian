package ws.palladian.classification;

import java.util.List;

import ws.palladian.processing.features.FeatureVector;

public interface Predictor<M extends Model> {

    // FIXME call that "train"
    M learn(List<NominalInstance> instances);

    // FIXME activate method below
    // M train(Dataset dataset);

    CategoryEntries predict(FeatureVector vector, M model);

}