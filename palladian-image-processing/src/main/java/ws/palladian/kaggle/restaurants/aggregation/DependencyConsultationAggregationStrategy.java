package ws.palladian.kaggle.restaurants.aggregation;

import ws.palladian.kaggle.restaurants.dataset.Label;
import ws.palladian.kaggle.restaurants.utils.DependencyMatrix;
import ws.palladian.kaggle.restaurants.utils.DependencyMatrixBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Use the dependencies between class labels to improve prediction.
 */
public class DependencyConsultationAggregationStrategy implements AggregationStrategy {

    /**
     * Label priors on a BUSINESS basis (not photo basis) and dependencies between labels.
     */
    private DependencyMatrix dependencyMatrix;

    public DependencyConsultationAggregationStrategy() {
        dependencyMatrix = new DependencyMatrixBuilder().buildMatrix();
    }

    @Override
    public Map<Label, Double> aggregate(Collection<Map<Label, Double>> classifiedImages) {

        Map<Label, Double> result = new HashMap<>();

        for (Label label : Label.values()) {
            result.put(label, 0.);
        }

        double maxScore = 1;
        for (Map<Label, Double> classifiedImage : classifiedImages) {
            for (Entry<Label, Double> labelProbability : classifiedImage.entrySet()) {

                Label label = labelProbability.getKey();
                Double probability = labelProbability.getValue();
                Double prior = dependencyMatrix.getPrior(label);
                Double score = prior * probability;

                // how likely are the other labels and what is their influence on this one?
                for (Entry<Label, Double> otherLabelProbability : classifiedImage.entrySet()) {
                    Label otherLabel = otherLabelProbability.getKey();
                    if (otherLabel == label) {
                        continue;
                    }
                    Double otherPrior = dependencyMatrix.getPrior(otherLabel);
                    Double otherProbability = otherLabelProbability.getValue();
                    Double dependency = dependencyMatrix.getDependency(otherLabel, label);
                    score += otherPrior * dependency * otherProbability;
//             score += dependency * otherProbability;
                }

                Double currentScore = result.get(labelProbability.getKey());
                Double newScore = currentScore + score;
                result.put(labelProbability.getKey(), newScore);

                if (newScore > maxScore) {
                    maxScore = newScore;
                }
            }
        }

        // normalize
        for (Label label : Label.values()) {
            result.put(label, result.get(label) / (maxScore * classifiedImages.size()));
        }

        return result;
    }

}
