package ws.palladian.model.features;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FeatureVector<F extends Feature<V>, V> implements FeatureValue, Iterable<F> {

    private Map<String, F> features;

    public FeatureVector() {
        features = new HashMap<String, F>();
    }

    public void add(F feature) {
        features.put(feature.getName(), feature);
    }

    @Override
    public Iterator<F> iterator() {
        return features.values().iterator();
    }

}
