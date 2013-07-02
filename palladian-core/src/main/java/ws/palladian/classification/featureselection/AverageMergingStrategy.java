/**
 * Created on: 20.05.2013 09:33:58
 */
package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.Trainable;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since
 */
public final class AverageMergingStrategy implements SelectedFeatureMergingStrategy {

    @Override
    public FeatureRanking merge(Collection<? extends Trainable> dataset, Collection<FeatureDetails> featuresToConsider) {
        FeatureRanking ranking = new FeatureRanking();
        for (FeatureDetails featureDetails : featuresToConsider) {
            Map<String, Map<String, Double>> scoredFeature = ChiSquaredFeatureSelector.calculateChiSquareValues(
                    featureDetails.getPath(), featureDetails.getType(), dataset);

            Validate.isTrue((!featureDetails.isSparse() && scoredFeature.size() == 1) || (featureDetails.isSparse()));

            // this should usually only run once for non sparse features.
            for (Entry<String, Map<String, Double>> scoredValue : scoredFeature.entrySet()) {
                double averageScore = 0.0d;

                for (Double value : scoredValue.getValue().values()) {
                    averageScore += value;
                }
                averageScore /= scoredValue.getValue().size();

                if (featureDetails.isSparse()) {
                    ranking.addSparse(featureDetails.getPath(), scoredValue.getKey(), averageScore);
                } else {
                    ranking.add(scoredValue.getKey(), averageScore);
                }
            }
        }
        return ranking;
    }

}
