/**
 * 
 */
package ws.palladian.classification.universal;

import java.util.List;

import ws.palladian.classification.Instance;

/**
 * @author Klemens Muthmann
 * @version 1.0.0
 * @since 0.1.8
 */
public interface UniversalClassifierWeightStrategy {
    void learnClassifierWeights(List<Instance> instances, UniversalClassifierModel model);
}
