/**
 * Created on: 20.05.2013 09:33:58
 */
package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.classification.Instance;

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
    public FeatureRanking merge(Collection<Instance> dataset) {
        FeatureRanking ranking = new FeatureRanking();
        Map<String, Map<String, Double>> scoredFeature = ChiSquaredFeatureRanker.calculateChiSquareValues(dataset);

        // this should usually only run once for non sparse features.
        for (Entry<String, Map<String, Double>> scoredValue : scoredFeature.entrySet()) {
            double averageScore = 0.0d;

            for (Double value : scoredValue.getValue().values()) {
                averageScore += value;
            }
            averageScore /= scoredValue.getValue().size();

            ranking.add(scoredValue.getKey(), averageScore);
        }
        return ranking;
    }

}
