package ws.palladian.classification.featureselection;

import java.util.Collection;

import ws.palladian.core.Instance;
import ws.palladian.helper.ProgressReporter;

/**
 * <p>
 * Base interface for all feature rankers. A feature ranker provides a score for all features in a dataset on how
 * valuable that feature is for classifying the dataset.
 * </p>
 * 
 * @author Klemens Muthmann
 */
public interface FeatureRanker {

    FeatureRanking rankFeatures(Collection<? extends Instance> dataset);
    
    FeatureRanking rankFeatures(Collection<? extends Instance> dataset, ProgressReporter progress);

}
