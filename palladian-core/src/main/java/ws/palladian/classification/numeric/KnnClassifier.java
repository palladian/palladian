package ws.palladian.classification.numeric;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.classification.utils.ClassificationUtils;
import ws.palladian.classification.utils.MinMaxNormalizer;
import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.collection.EntryValueComparator;
import ws.palladian.helper.math.NumericVector;

/**
 * <p>
 * A KNN (k-nearest neighbor) classifier. It classifies {@link FeatureVector}s based on the k nearest {@link Instance}s
 * from a {@link KnnModel} created by a {@link KnnLearner}. Since this is an instance based classifier, it is fast
 * during the learning phase but has a more complicated prediction phase.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class KnnClassifier implements Classifier<KnnModel> {

    /**
     * <p>
     * Number of nearest neighbors that are allowed to vote. If neighbors have the same distance they will all be
     * considered for voting, k might increase in these cases.
     * </p>
     */
    private final int k;

    /**
     * <p>
     * Creates a new completely initialized KNN classifier with specified k using a {@link MinMaxNormalizer}. A typical
     * value for k is 3. This constructor should be used if the created object is used for prediction.
     * </p>
     * 
     * @param k The parameter k specifying the k nearest neighbors to use for classification. Must be greater zero.
     */
    public KnnClassifier(int k) {
        Validate.isTrue(k > 0, "k must be greater zero");
        this.k = k;
    }

    /**
     * <p>
     * Creates a new completely initialized KNN classifier with a k of 3 and a {@link MinMaxNormalizer}. This
     * constructor should typically be used if the class is used for learning. In that case the value of k is not
     * important. It is only used during prediction.
     * </p>
     */
    public KnnClassifier() {
        this(3);
    }

    @Override
    public CategoryEntries classify(FeatureVector featureVector, KnnModel model) {

        FeatureVector normalizedFeatureVector = model.getNormalization().normalize(featureVector);

        // initialize with all category names and a score of zero
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder().set(model.getCategories(), 0);

        NumericVector<String> numericVector = ClassificationUtils.getNumericVector(normalizedFeatureVector);

        // find k nearest neighbors, compare instance to every known instance
        List<Pair<String, Double>> neighbors = CollectionHelper.newArrayList();
        for (TrainingExample example : model.getTrainingExamples()) {
            double distance = example.getVector().euclidean(numericVector);
            neighbors.add(Pair.of(example.category, distance));
        }

        // sort near neighbor map by distance
        Collections.sort(neighbors, new EntryValueComparator<Double>(Order.ASCENDING));

        // if there are several instances at the same distance we take all of them into the voting, k might get bigger
        // in those cases
        double lastDistance = -1;
        int ck = 0;
        for (Pair<String, Double> neighbor : neighbors) {
            if (ck >= k && neighbor.getValue() != lastDistance) {
                break;
            }
            double weight = 1.0 / (neighbor.getValue() + 0.000000001);
            String targetClass = neighbor.getKey();
            builder.add(targetClass, weight);
            lastDistance = neighbor.getValue();
            ck++;
        }

        return builder.create();
    }

}
