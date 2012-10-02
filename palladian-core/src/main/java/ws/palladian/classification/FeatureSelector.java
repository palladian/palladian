/**
 * Created on: 02.10.2012 17:27:44
 */
package ws.palladian.classification;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.8
 */
public final class FeatureSelector {

    /**
     * <p>
     * This is a static utility class. The constructor should never be called.
     * </p>
     */
    private FeatureSelector() {
        throw new UnsupportedOperationException("Unable to instantiate static utility class");
    }

    // chi square test funktioniert nur für nominelle merkmale
    public static <T> Map<T, Double> calculateChiSquareValues(ListFeatureDescriptor<T> descriptor,
            Collection<Instance2<String>> instances) {
        WordCorrelationMatrix termClassCorrelationMatrix = new WordCorrelationMatrix();

        for (Instance2<String> instance : instances) {
            T value = instance.featureVector.get(descriptor).getValue();
            termClassCorrelationMatrix.updatePair(value.toString(), instance.target);
        }
    }

    public static Map<String, Double> calculateChiSquareValues(NominalFeatureDescriptor descriptor,
            Collection<Instance2<String>> instances) {
        Map<String, Map<String, Integer>> termClassCorrelationMatrix = new HashMap<String, Map<String, Integer>>();
        Map<String, Double> ret = new HashMap<String, Double>();

        for (Instance2<String> instance : instances) {
            String value = instance.featureVector.get(descriptor).getValue();
            Map<String, Integer> correlations = termClassCorrelationMatrix.get(value);
            if (correlations == null) {
                correlations = new HashMap<String, Integer>();
            }
            Integer occurenceCount = correlations.get(instance.target);
            if (occurenceCount == null) {
                occurenceCount = 0;
            }
            occurenceCount++;
            correlations.put(instance.target, occurenceCount);
            termClassCorrelationMatrix.put(value, correlations);
        }

        for (Map.Entry<String, Map<String, Integer>> termOccurence : termClassCorrelationMatrix.entrySet()) {
            ret.put(termOccurence.getKey(), value);
        }

        return ret;
    }

    // pca funktioniert nur für numerische merkmale

}
