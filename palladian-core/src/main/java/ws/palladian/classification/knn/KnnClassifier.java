package ws.palladian.classification.knn;

import it.unimi.dsi.fastutil.objects.Object2FloatMap;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;
import ws.palladian.core.*;
import ws.palladian.core.value.NominalValue;
import ws.palladian.core.value.NumericValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.collection.EntryValueComparator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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

    @Override
    public CategoryEntries classify(FeatureVector inputVector, KnnModel model) {
        // initialize with all category names and a score of zero
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder().set(model.getCategories(), 0);

        List<Pair<String, Double>> neighbors = new ArrayList<>();

        for (String y : model.getRowKeys()) {
            FeatureVector trainedVector = model.getFeatureVector(y);
            double distance = computeDistance(inputVector, trainedVector, model);
            neighbors.add(Pair.of(trainedVector.getNominal("category").getString(), distance));
        }

        neighbors.sort(new EntryValueComparator<>(Order.ASCENDING));
        List<Pair<String, Double>> sublist = neighbors.subList(0, k);

        for (Pair<String, Double> neighbor : sublist) {
            double distance = neighbor.getValue();
            double weight = 1.0 / (distance + 0.000000001);
            String targetClass = neighbor.getKey();
            builder.add(targetClass, weight);
        }

        return builder.create();
    }

    private double computeDistance(FeatureVector inputVector, FeatureVector trainedVector, KnnModel model) {
        Objects.requireNonNull(inputVector, "otherNumeric must not be null");
        Objects.requireNonNull(trainedVector, "otherTextual must not be null");

        double distance = 0;
        for (Object2FloatMap.Entry<String> entry : model.getNumericFieldsAndWeights().object2FloatEntrySet()) {
            String numericFieldLabel = entry.getKey();
            Value inputValue = inputVector.get(numericFieldLabel);
            Value trainedValue = trainedVector.get(numericFieldLabel);

            if (inputValue.isNull() || trainedValue.isNull()) {
                distance += entry.getFloatValue();
            } else if (inputValue instanceof NumericValue && trainedValue instanceof NumericValue) {
                float diff = ((NumericValue) inputValue).getFloat() - ((NumericValue) trainedValue).getFloat();
                distance += entry.getFloatValue() * diff * diff;
            }
        }

        // XXX
        // the idea is to make the penalty for non-matches relative to the difference in numeric values
        // this is now a different value per sample which doesn't really make sense but evaluation shows it works better than static values
        for (Object2FloatMap.Entry<String> entry : model.getTextualFieldsAndWeights().object2FloatEntrySet()) {
            String fieldLabel = entry.getKey();
            Value inputValue = inputVector.get(fieldLabel);
            Value trainedValue = trainedVector.get(fieldLabel);

            if (inputValue instanceof NominalValue && trainedValue instanceof NominalValue) {
                if (!((NominalValue) inputValue).getString().equals(((NominalValue) trainedValue).getString())) {
                    distance += entry.getFloatValue();
                }
            } else {
                distance += entry.getFloatValue();
            }
        }

        return distance;
    }

    public List<String> getNeighbors(FeatureVector instance, KnnModel model) {
        return getNeighbors(instance, model, k);
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
