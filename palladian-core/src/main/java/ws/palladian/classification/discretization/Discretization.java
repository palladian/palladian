package ws.palladian.classification.discretization;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
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

    public FeatureVector discretize(FeatureVector featureVector, final Collection<? extends Instance> dataset) {
        InstanceBuilder instanceBuilder = new InstanceBuilder();

        for (VectorEntry<String, Value> entry : featureVector) {
            final String name = entry.key();
            Value value = entry.value();
            if (value instanceof NumericValue) {
                NumericValue numericValue = (NumericValue)value;
                Binner binner = binnerCache.get(name);
                if (binner == null) {
                    binner = discretize(name, dataset, new Comparator<Instance>() {

                        @Override
                        public int compare(Instance i1, Instance i2) {
                            NumericValue value1 = (NumericValue)i1.getVector().get(name);
                            NumericValue value2 = (NumericValue)i2.getVector().get(name);
                            Double i1FeatureValue = value1 == null ? Double.MIN_VALUE : value1.getDouble();
                            Double i2FeatureValue = value2 == null ? Double.MIN_VALUE : value2.getDouble();
                            return i1FeatureValue.compareTo(i2FeatureValue);
                        }
                    });
                    binnerCache.put(name, binner);
                }
                instanceBuilder.set(name, binner.bin(numericValue));
            } else {
                instanceBuilder.set(name, value);
            }
        }
        return instanceBuilder.create();
    }

    /**
     * <p>
     * Descretize {@link NumericFeature}s following the algorithm proposed by Fayyad and Irani in
     * "Multi-Interval Discretization of Continuous-Valued Attributes for Classification Learning."
     * </p>
     * 
     * @param featureName The path to the {@link NumericFeature} to discretize.
     * @param dataset The dataset to base the discretization on. The provided {@link Instance}s should contain the
     *            feature for this algorithm to work.
     * @return A {@link Binner} object capable of discretization of already encountered and unencountered values for the
     *         provided {@link NumericFeature}.
     */
    private static Binner discretize(final String featureName, Collection<? extends Instance> dataset,
            Comparator<Instance> comparator) {
        List<Instance> sortedInstances = new ArrayList<Instance>(dataset);
        Collections.sort(sortedInstances, comparator);
        return Binner.createBinner(sortedInstances, featureName);
    }

}
