package ws.palladian.classification.featureselection;

import ws.palladian.helper.math.NumericMatrix;

/**
 * <p>
 * Strategy for merging the features selected by the {@link ChiSquaredFeatureRanker} for each class. If you find it hard
 * to decide which implementation to use, you probably would want to try the {@link AverageMergingStrategy}, which just
 * computes the final ranking based on the arithmetic mean of the per class scores for each feature.
 * </p>
 * 
 * @author Klemens Muthmann
 */
public interface SelectedFeatureMergingStrategy {
    /**
     * Merges the ranked features provided by {@code rankedFeaturesPerClass} into a {@link FeatureRanking}.
     * 
     * @param chiSquareMatrix The chi-squared scores to merge with this merging strategy.
     * @return A merged ranking of the provided features.
     */
    FeatureRanking merge(NumericMatrix<String> chiSquareMatrix);
}
