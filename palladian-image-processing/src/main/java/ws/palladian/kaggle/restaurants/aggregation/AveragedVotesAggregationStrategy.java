package ws.palladian.kaggle.restaurants.aggregation;

import ws.palladian.kaggle.restaurants.dataset.Label;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Probabilities for each label are averaged and then compared to the threshold.
 */
public class AveragedVotesAggregationStrategy implements AggregationStrategy {

    @Override
    public Map<Label, Double> aggregate(Collection<Map<Label, Double>> classifiedImages) {
        Map<Label, Double> result = new HashMap<>();

        for (Label label : Label.values()) {
            result.put(label, 0.);
        }

        for (Map<Label, Double> classifiedImage : classifiedImages) {
            for (Entry<Label, Double> labelProbability : classifiedImage.entrySet()) {
                Double currentProbabilitySum = result.get(labelProbability.getKey());
                result.put(labelProbability.getKey(), currentProbabilitySum + labelProbability.getValue());
            }
        }

        for (Label label : Label.values()) {
            result.put(label, result.get(label) / classifiedImages.size());
        }

        return result;
    }

}
