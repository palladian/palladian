/**
 * Created on: 02.10.2012 17:27:44
 */
package ws.palladian.classification.featureselection;

import java.util.Collection;

import ws.palladian.processing.Trainable;

/**
 * <p>
 * Base interface for all feature selectors.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 2.0
 * @since 0.1.8
 */
public interface FeatureSelector {

    FeatureRanking rankFeatures(Collection<? extends Trainable> dataset, Collection<FeatureDetails> featuresToConsider);
}
