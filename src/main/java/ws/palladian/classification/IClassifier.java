package ws.palladian.classification;

import ws.palladian.model.features.FeatureVector;

public interface IClassifier {

    CategoryEntries classify(FeatureVector featureVector);

}
