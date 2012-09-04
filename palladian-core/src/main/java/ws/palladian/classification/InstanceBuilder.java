package ws.palladian.classification;

import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class InstanceBuilder {
    
    private final FeatureVector featureVector;

    public InstanceBuilder() {
        this.featureVector = new FeatureVector();
    }
    
    public InstanceBuilder set(String name, String value) {
        featureVector.add(new NominalFeature(name, value));
        return this;
    }
    
    public InstanceBuilder set(String name, Double value) {
        featureVector.add(new NumericFeature(name, value));
        return this;
    }
    
    public NominalInstance create(String target) {
        NominalInstance instance = new NominalInstance();
        instance.featureVector = featureVector;
        instance.target = target;
        return instance;
    }
    
    public FeatureVector create() {
        return featureVector;
    }

}
