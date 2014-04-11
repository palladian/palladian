package ws.palladian.classification.featureselection;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.math.NumericMatrix;
import ws.palladian.helper.math.NumericMatrix.NumericMatrixVector;

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
    public FeatureRanking merge(NumericMatrix<String> chiSquareMatrix) {
        FeatureRanking ret = new FeatureRanking();
        Map<String, FeatureRanking> rankingsPerTargetClass = new HashMap<String, FeatureRanking>();

        // this should usually only run once for non sparse features.
        for (NumericMatrixVector<String> scoredValue : chiSquareMatrix.rows()) {

            for (VectorEntry<String, Double> entry : scoredValue) {
                FeatureRanking rankingPerTargetClass = rankingsPerTargetClass.get(entry.key());
                if (rankingPerTargetClass == null) {
                    rankingPerTargetClass = new FeatureRanking();
                }
                rankingPerTargetClass.add(scoredValue.key(), entry.value());
                rankingsPerTargetClass.put(entry.key(), rankingPerTargetClass);
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
