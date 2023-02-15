package ws.palladian.kaggle.restaurants.aggregation;

import ws.palladian.kaggle.restaurants.dataset.Label;

import java.util.Collection;
import java.util.Map;

public interface AggregationStrategy {
    /**
     * Aggregate the classification of individual images for one business.
     *
     * @param classifiedImages The classified images with the probability value for each
     *                         label.
     * @return The aggregated probabilities for each label.
     */
    Map<Label, Double> aggregate(Collection<Map<Label, Double>> classifiedImages);
}
