package ws.palladian.classification.discretization;

import java.util.Collection;
import java.util.Map;

import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.core.InstanceBuilder;
import ws.palladian.core.NumericValue;
import ws.palladian.core.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;

public class Discretization {

    private final Map<String, Binner> binnerCache = CollectionHelper.newHashMap();

    public FeatureVector discretize(FeatureVector featureVector, Collection<? extends Instance> dataset) {
        InstanceBuilder instanceBuilder = new InstanceBuilder();

        for (VectorEntry<String, Value> vectorEntry : featureVector) {
            String featureName = vectorEntry.key();
            Value featureValue = vectorEntry.value();

            if (featureValue instanceof NumericValue) {
                NumericValue numericValue = (NumericValue)featureValue;
                Binner binner = binnerCache.get(featureName);
                if (binner == null) {
                    binner = Binner.createBinner(dataset, featureName);
                    binnerCache.put(featureName, binner);
                }
                instanceBuilder.set(featureName, binner.bin(numericValue));
            } else {
                instanceBuilder.set(featureName, featureValue);
            }
        }
        return instanceBuilder.create();
    }

}
