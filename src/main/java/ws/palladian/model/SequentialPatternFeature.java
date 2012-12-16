package ws.palladian.model;

import ws.palladian.model.features.Feature;
import ws.palladian.model.features.FeatureDescriptor;

public class SequentialPatternFeature extends Feature<SequentialPattern> {

    /**
     * @param descriptor
     * @param value
     */
    public SequentialPatternFeature(FeatureDescriptor<Feature<SequentialPattern>> descriptor, SequentialPattern value) {
        super(descriptor, value);
    }

    /**
     * @param name
     * @param value
     */
    public SequentialPatternFeature(String name, SequentialPattern value) {
        super(name, value);
    }

}
