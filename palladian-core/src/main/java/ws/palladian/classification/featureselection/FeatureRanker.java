/**
 * Created on: 02.10.2012 17:27:44
 */
package ws.palladian.classification.featureselection;

import java.util.Collection;

import ws.palladian.core.Instance;

/**
 * <p>
 * Base interface for all feature rankers. A feature ranker provides a score for all features in a dataset on how
 * valuable that feature is for classifying the dataset.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 3.0
 * @since 0.1.8
 */
public interface FeatureRanker {

    FeatureRanking rankFeatures(Collection<? extends Instance> dataset);

}
