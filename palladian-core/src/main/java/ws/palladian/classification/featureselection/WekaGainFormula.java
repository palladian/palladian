/**
 * Created on 07.08.2013 15:49:11
 */
package ws.palladian.classification.featureselection;

import java.util.List;

import ws.palladian.processing.Classified;
import ws.palladian.processing.features.Feature;

/**
 * <p>
 * Implements the Information Gain formula as proposed by the Weka Machine Learning Framework.
 * </p>
 *
 * @author Klemens Muthmann
 * @version 1.0
 * @since 2.1.0
 */
public final class WekaGainFormula {
    
    public double calculateGain(List<Classified> dataset, List<String> targetClasses, Feature<?> feature) {
        double ret = 0.0;
        for(String targetClass:targetClasses) {
            ret += entropy(dataset,targetClass) - entropy(dataset,targetClass,feature);
        }
        return ret/targetClasses.size();
    }
    
    private double entropy(List<Classified> dataset, String targetClass) {
        return 0.0d;
    }
    
    private double entropy(List<Classified> dataset, String targetClass, Feature<?> feature) {
        return 0.0d;
    }
}
