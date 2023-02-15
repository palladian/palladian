package ws.palladian.kaggle.restaurants.aggregation;

import ws.palladian.kaggle.restaurants.dataset.Label;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * If one image has a certain label, the entire result gets that label.
 */
public class OneVoteThresholdAggregationStrategy implements AggregationStrategy {

    private final double threshold;

    public OneVoteThresholdAggregationStrategy(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public Map<Label, Double> aggregate(Collection<Map<Label, Double>> classifiedImages) {
        Map<Label, Double> result = new HashMap<>();
        for (Map<Label, Double> classifiedImage : classifiedImages) {
            for (Entry<Label, Double> labelProbability : classifiedImage.entrySet()) {
                if (labelProbability.getValue() > threshold) {
                    // is the current vote even stronger than an existing one?
                    Double highestProbability = result.get(labelProbability.getKey());
                    if (highestProbability == null || (labelProbability.getValue() > highestProbability)) {
                        result.put(labelProbability.getKey(), labelProbability.getValue());
                    }
                }
            }
        }
        return result;
    }

}
