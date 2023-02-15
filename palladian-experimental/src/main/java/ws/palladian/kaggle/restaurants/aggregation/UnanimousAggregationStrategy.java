package ws.palladian.kaggle.restaurants.aggregation;

import ws.palladian.helper.collection.Bag;
import ws.palladian.kaggle.restaurants.dataset.Label;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * The label is only given if all images agree that it should get it. This is the opposite of the OneVoteThresholdAggregationStrategy.
 */
public class UnanimousAggregationStrategy implements AggregationStrategy {

    private final double threshold;

    public UnanimousAggregationStrategy(double threshold) {
        this.threshold = threshold;
    }

    @Override
    public Map<Label, Double> aggregate(Collection<Map<Label, Double>> classifiedImages) {
        Map<Label, Double> result = new HashMap<>();

        for (Label label : Label.values()) {
            result.put(label, 0.);
        }

        Bag<Label> votes = new Bag<>();
        for (Map<Label, Double> classifiedImage : classifiedImages) {
            for (Entry<Label, Double> labelProbability : classifiedImage.entrySet()) {
                if (labelProbability.getValue() > threshold) {
                    votes.add(labelProbability.getKey());
                    Double currentProbabilitySum = result.get(labelProbability.getKey());
                    result.put(labelProbability.getKey(), currentProbabilitySum + labelProbability.getValue());
                }
            }
        }

        for (Label label : Label.values()) {
            // unanimous decision?
            if (votes.count(label) == classifiedImages.size()) {
                result.put(label, result.get(label) / classifiedImages.size());
            } else {
                result.put(label, 0.);
            }
        }

        return result;
    }

}
