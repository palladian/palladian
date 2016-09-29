package ws.palladian.classification.featureselection;

import java.util.Collection;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.Dataset;
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

	/** @deprecated Use {@link #rankFeatures(Dataset)} instead. */
	@Deprecated
    FeatureRanking rankFeatures(Collection<? extends Instance> dataset);
    
    FeatureRanking rankFeatures(Dataset dataset);
    
    /** @deprecated Use {@link #rankFeatures(Dataset)} instead. */
    @Deprecated
    FeatureRanking rankFeatures(Collection<? extends Instance> dataset, ProgressReporter progress);
    
    FeatureRanking rankFeatures(Dataset dataset, ProgressReporter progress);
    
    FeatureRanking rankFeatures(Dataset trainSet, Dataset validationSet, ProgressReporter progress);
    
    FeatureRanking rankFeatures(Dataset trainSet, Dataset validationSet);

}
