package ws.palladian.classification;

import ws.palladian.processing.features.BasicFeatureVector;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

public class InstanceBuilder {

    // TODO auto-generate feature names if not explicitly given.

    private final FeatureVector featureVector;

    public InstanceBuilder() {
        this.featureVector = new BasicFeatureVector();
    }

    public InstanceBuilder set(String name, String value) {
        featureVector.add(new NominalFeature(name, value));
        return this;
    }

    public InstanceBuilder set(String name, boolean value) {
        featureVector.add(new NominalFeature(name, String.valueOf(value)));
        return this;
    }

    public InstanceBuilder set(String name, Number value) {
        featureVector.add(new NumericFeature(name, value));
        return this;
    }

    public Instance create(String target) {
        return new Instance(target, featureVector);
    }

    public Instance create(boolean target) {
        return new Instance(target, featureVector);
    }

    public FeatureVector create() {
        return featureVector;
    }

}
