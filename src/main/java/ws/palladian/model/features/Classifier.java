package ws.palladian.model.features;

import ws.palladian.classification.CategoryEntries;

public interface Classifier {

    CategoryEntries classify(FeatureVector featureVector);

}
