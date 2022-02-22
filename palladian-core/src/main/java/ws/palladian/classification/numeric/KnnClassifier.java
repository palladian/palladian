package ws.palladian.classification.numeric;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import ws.palladian.core.*;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.collection.EntryValueComparator;
import ws.palladian.helper.collection.FixedSizePriorityQueue;

import java.util.ArrayList;
import java.util.List;

/**
 * A KNN (k-nearest neighbor) classifier. It classifies {@link FeatureVector}s
 * based on the k nearest {@link Instance}s from a {@link KnnModel} created by a
 * {@link KnnLearner}. Since this is an instance based classifier, it is fast
 * during the learning phase but has a more expensive prediction phase.
 *
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class KnnClassifier implements Classifier<KnnModel> {
    /** Number of nearest neighbors that are allowed to vote. */
    private int k;

    /** Whether to use textual features. */
    private boolean useTextualFeatures;

    /**
     * Create a KNN classifier with specified k.
     *
     * @param k The number of nearest neighbors used for voting. Greater zero,
     *          typical value is 3.
     */
    public KnnClassifier(int k) {
        Validate.isTrue(k > 0, "k must be greater zero");
        this.k = k;
    }

    /**
     * Create a KNN classifier with specified k.
     *
     * @param k                  The number of nearest neighbors used for voting. Greater zero,
     *                           typical value is 3.
     * @param useTextualFeatures Whether to use the textual features.
     */
    public KnnClassifier(int k, boolean useTextualFeatures) {
        Validate.isTrue(k > 0, "k must be greater zero");
        this.k = k;
        this.useTextualFeatures = useTextualFeatures;
    }

    /**
     * Create a KNN classifier with a k of 3.
     */
    public KnnClassifier() {
        this(3);
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, KnnModel model) {
        // initialize with all category names and a score of zero
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder().set(model.getCategories(), 0);

        double[] numericVector = model.getNormalizedVectorForClassification(featureVector);
        String[] stringVector = model.getStringVectorForClassification(featureVector);

        // find k nearest neighbors, compare instance to every known instance
        FixedSizePriorityQueue<Pair<String, Double>> neighbors = new FixedSizePriorityQueue<>(k, new EntryValueComparator<>(Order.DESCENDING));

        for (TrainingExample example : model.getTrainingExamples()) {
            double distance;
            if (useTextualFeatures) {
                distance = example.distance(numericVector, stringVector);
            } else {
                distance = example.distance(numericVector);
            }
            neighbors.add(Pair.of(example.category, distance));
        }

        for (Pair<String, Double> neighbor : neighbors.asList()) {
            double distance = neighbor.getValue();
            double weight = 1.0 / (distance + 0.000000001);
            String targetClass = neighbor.getKey();
            builder.add(targetClass, weight);
        }

        return builder.create();
    }

    public List<String> getNeighbors(FeatureVector instance, KnnModel model, int numNeighbors) {
        int originalK = k;
        k = numNeighbors;
        List<String> categoryNames = new ArrayList<>();
        CategoryEntries classify = classify(instance, model);
        for (Category category : classify) {
            categoryNames.add(category.getName());
        }
        k = originalK;
        return CollectionHelper.getSublist(categoryNames, 0, numNeighbors);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (k=" + k + ")";
    }
}
