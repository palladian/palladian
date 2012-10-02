/**
 * Created on: 02.10.2012 17:27:44
 */
package ws.palladian.classification;

import java.util.Collection;
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
        WordCorrelationMatrix termClassCorrelationMatrix = new WordCorrelationMatrix();

        for (Instance2<String> instance : instances) {
            String value = instance.featureVector.get(descriptor).getValue();
            termClassCorrelationMatrix.updatePair(value.toString(), instance.target);
        }
    }

    // pca funktioniert nur für numerische merkmale

}
