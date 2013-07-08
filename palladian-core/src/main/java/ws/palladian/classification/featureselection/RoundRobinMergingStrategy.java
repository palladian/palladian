/**
 * Created on: 20.05.2013 10:21:22
 */
package ws.palladian.classification.featureselection;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.classification.Instance;

/**
 * <p>
 * Merges feature rankings for multiple classes by always selecting the top N per class.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.1
 */
public final class RoundRobinMergingStrategy implements SelectedFeatureMergingStrategy {

    @Override
    public FeatureRanking merge(Collection<Instance> dataset, Map<String,Map<String,Double>> chiSquaredValues) {
        FeatureRanking ret = new FeatureRanking();
        Map<String, FeatureRanking> rankingsPerTargetClass = new HashMap<String, FeatureRanking>();

        // this should usually only run once for non sparse features.
        for (Entry<String, Map<String, Double>> scoredValue : chiSquaredValues.entrySet()) {

            for (Entry<String, Double> entry : scoredValue.getValue().entrySet()) {
                FeatureRanking rankingPerTargetClass = rankingsPerTargetClass.get(entry.getKey());
                if (rankingPerTargetClass == null) {
                    rankingPerTargetClass = new FeatureRanking();
                }
                rankingPerTargetClass.add(scoredValue.getKey(), entry.getValue());
                rankingsPerTargetClass.put(entry.getKey(), rankingPerTargetClass);
            }
        }

        // do round robin ordering of features.
        int maxIndex = 0;
        for (Entry<String, FeatureRanking> rankingPerTargetClass : rankingsPerTargetClass.entrySet()) {
            maxIndex = Math.max(maxIndex, rankingPerTargetClass.getValue().size());
        }

        for (int i = 0; i < maxIndex; i++) {
            for (Entry<String, FeatureRanking> rankingPerTargetClass : rankingsPerTargetClass.entrySet()) {
                List<RankedFeature> rankedFeatures = rankingPerTargetClass.getValue().getAll();
                if (i < rankedFeatures.size()) {
                    RankedFeature rankedFeature = rankedFeatures.get(i);

                    if (!ret.getAll().contains(rankedFeature)) {
                        ret.addSparse(rankedFeature.getIdentifier(), rankedFeature.getValue(),
                                Integer.valueOf(maxIndex - i).doubleValue());
                    }
                }
            }
        }

        return ret;
    }

}
