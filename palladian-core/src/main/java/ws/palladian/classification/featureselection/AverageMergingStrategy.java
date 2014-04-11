package ws.palladian.classification.featureselection;

import ws.palladian.helper.math.NumericMatrix;
import ws.palladian.helper.math.NumericMatrix.NumericMatrixVector;

/**
 * <p>
 * Merges the features that are ranked per class by the chi squared feature ranking strategy by averaging the scores
 * achieved by a feature for each class.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.2
 */
public final class AverageMergingStrategy implements SelectedFeatureMergingStrategy {

    @Override
    public FeatureRanking merge(NumericMatrix<String> chiSquareMatrix) {
        FeatureRanking ranking = new FeatureRanking();
        for (NumericMatrixVector<String> scoredValue : chiSquareMatrix.rows()) {
            double averageScore = scoredValue.sum() / scoredValue.size();
            ranking.add(scoredValue.key(), averageScore);
        }
        return ranking;
    }

}
