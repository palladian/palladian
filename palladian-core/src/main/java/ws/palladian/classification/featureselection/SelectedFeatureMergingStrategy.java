/**
 * Created on: 20.05.2013 09:31:12
 */
package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.Map;

import ws.palladian.classification.Instance;

/**
 * <p>
 * Strategy for merging the features selected by the {@link ChiSquaredFeatureRanker} for each class. If you find it
 * hard to decide which implementation to use, you probably would want to try the {@link AverageMergingStrategy}, which
 * just computes the final ranking based on the arithmetic mean of the per class scores for each feature.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.1
 */
public interface SelectedFeatureMergingStrategy {
    /**
     * <p>
     * Merges the ranked features provided by {@code rankedFeaturesPerClass} into a {@link FeatureRanking}.
     * </p>
     * 
     * @param rankedFeaturesPerClass The ranked features per target class. The first key is the feature mapped to a
     *            {@link Map} of target class to score.
     * @return A merged ranking of the provided features.
     */
    FeatureRanking merge(Collection<Instance> dataset);
}
