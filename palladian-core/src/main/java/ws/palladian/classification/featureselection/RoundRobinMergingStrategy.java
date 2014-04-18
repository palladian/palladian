package ws.palladian.classification.featureselection;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.collection.LazyMap;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.math.NumericMatrix;
import ws.palladian.helper.math.NumericMatrix.NumericMatrixVector;

/**
 * <p>
 * Merges feature rankings for multiple classes by always selecting the top N per class.
 * </p>
 * 
 * @author Klemens Muthmann
 */
public final class RoundRobinMergingStrategy implements SelectedFeatureMergingStrategy {

    @Override
    public FeatureRanking merge(NumericMatrix<String> chiSquareMatrix) {
        Map<String, FeatureRanking> rankingsPerTargetClass = LazyMap.create(new Factory<FeatureRanking>() {
            @Override
            public FeatureRanking create() {
                return new FeatureRanking();
            }
        });

        // this should usually only run once for non sparse features.
        for (NumericMatrixVector<String> scoredValue : chiSquareMatrix.rows()) {
            for (VectorEntry<String, Double> entry : scoredValue) {
                String categoryName = entry.key();
                String featureValueIdentifier = scoredValue.key();
                featureValueIdentifier = featureValueIdentifier.split("###")[0];
                rankingsPerTargetClass.get(categoryName).add(featureValueIdentifier, entry.value());
            }
        }

        // do round robin ordering of features.
        int maxIndex = 0;
        for (Entry<String, FeatureRanking> rankingPerTargetClass : rankingsPerTargetClass.entrySet()) {
            maxIndex = Math.max(maxIndex, rankingPerTargetClass.getValue().size());
        }

        FeatureRanking ret = new FeatureRanking();
        for (int i = 0; i < maxIndex; i++) {
            for (Entry<String, FeatureRanking> rankingPerTargetClass : rankingsPerTargetClass.entrySet()) {
                List<RankedFeature> rankedFeatures = rankingPerTargetClass.getValue().getAll();
                if (i < rankedFeatures.size()) {
                    RankedFeature rankedFeature = rankedFeatures.get(i);
                    if (ret.getFeature(rankedFeature.getName()) == null) {
                        ret.add(rankedFeature.getName(), maxIndex - i);
                    }
                }
            }
        }
        return ret;
    }

}
