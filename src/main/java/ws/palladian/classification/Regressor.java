package ws.palladian.classification;

import ws.palladian.model.features.FeatureVector;

public interface Regressor {

    double regress(FeatureVector featureVector);

}
