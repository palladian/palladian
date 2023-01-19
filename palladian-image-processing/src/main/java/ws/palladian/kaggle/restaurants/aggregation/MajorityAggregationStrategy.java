package ws.palladian.kaggle.restaurants.aggregation;

import ws.palladian.helper.collection.Bag;
import ws.palladian.kaggle.restaurants.dataset.Label;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Simple majority vote. Every label with its probability above the threshold counts as one vote. The more people vote the higher the score.
 */
public class MajorityAggregationStrategy implements AggregationStrategy {

    private final Map<Label, Double> thresholds;

    public MajorityAggregationStrategy(Map<Label, Double> thresholds) {
        this.thresholds = thresholds;
    }

    public MajorityAggregationStrategy(double threshold) {
        thresholds = new HashMap<>();
        for (Label label : Label.values()) {
            thresholds.put(label, threshold);
        }
    }

    @Override
    public Map<Label, Double> aggregate(Collection<Map<Label, Double>> classifiedImages) {
        Bag<Label> votes = new Bag();
        for (Map<Label, Double> classifiedImage : classifiedImages) {
            for (Entry<Label, Double> labelProbability : classifiedImage.entrySet()) {
                Double threshold = thresholds.get(labelProbability.getKey());
                if (labelProbability.getValue() > threshold) {
                    votes.add(labelProbability.getKey());
                }
            }
        }
        Map<Label, Double> result = new HashMap<>();
        for (Entry<Label, Integer> labelCount : votes.unique()) {
            Double score = (double) labelCount.getValue() / classifiedImages.size();
            result.put(labelCount.getKey(), score);
        }
        return result;
    }

}
