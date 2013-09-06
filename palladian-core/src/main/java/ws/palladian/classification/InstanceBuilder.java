package ws.palladian.classification;

import ws.palladian.processing.features.BasicFeatureVectorImpl;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class InstanceBuilder {
    
    // TODO auto-generate feature names if not explicitly given.
    
    private final BasicFeatureVectorImpl featureVector;

    public InstanceBuilder() {
        this.featureVector = new BasicFeatureVectorImpl();
    }
    
    public InstanceBuilder set(String name, String value) {
        featureVector.add(new NominalFeature(name, value));
        return this;
    }
    
    public InstanceBuilder set(String name, Number value) {
        featureVector.add(new NumericFeature(name, value));
        return this;
    }
    
    public Instance create(String target) {
        Instance instance = new Instance(target, featureVector);
        return instance;
    }
    
    public BasicFeatureVectorImpl create() {
        return featureVector;
    }

}
