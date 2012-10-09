package ws.palladian.classification.numeric;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Classifier;
import ws.palladian.classification.Instance;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.EntryValueComparator;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * A KNN (k-nearest neighbor) classifier. It classifies {@link FeatureVector}s based on the k nearest {@link Instance} s
 * from the training set. Since this is an instance based classifier, it is fast during the learning phase but has a
 * more complicated prediction phase.
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
     * Creates a new completely initialized KNN classifier with specified k. A typical value is 3. This constructor
     * should be used if the created object is used for prediction.
     * </p>
     * 
     * @param k The parameter k specifying the k nearest neighbors to use for classification.
     */
    public KnnClassifier(Integer k) {
        this.k = k;
    }

    /**
     * <p>
     * Creates a new completely initialized KNN classifier with a k of 3. This constructor should typically be used if
     * the class is used for learning. In that case the value of k is not important. It is only used during prediction.
     * </p>
     */
    public KnnClassifier() {
        this(3);
    }

    @Override
    public KnnModel train(List<Instance> instances) {
        return new KnnModel(instances);
    }

    @Override
    public CategoryEntries classify(FeatureVector vector, KnnModel model) {

        Categories categories = getPossibleCategories(model.getTrainingExamples());

        // we need to normalize the new instance if the training instances were also normalized
        if (model.isNormalized()) {
            model.normalize(vector);
        }

        CategoryEntries bestFitList = new CategoryEntries();

        // create one category entry for every category with relevance 0
        for (Category category : categories) {
            bestFitList.add(new CategoryEntry(bestFitList, category, 0));
        }

        // find k nearest neighbors, compare instance to every known instance
        List<Pair<Instance, Double>> neighbors = CollectionHelper.newArrayList();
        for (Instance knownInstance : model.getTrainingExamples()) {
            double distance = getDistanceBetween(vector, knownInstance.featureVector);
            neighbors.add(Pair.of(knownInstance, distance));
        }

        // sort near neighbor map by distance
        Collections.sort(neighbors, EntryValueComparator.<Instance, Double> ascending());

        int ck = 0;

        // if there are several instances at the same distance we take all of them into the voting, k might get bigger
        // in those cases
        double lastDistance = -1;
        for (Pair<Instance, Double> neighbor : neighbors) {

            if (ck >= k && neighbor.getValue() != lastDistance) {
                break;
            }

            CategoryEntry c = bestFitList.getCategoryEntry(neighbor.getKey().targetClass);
            c.addAbsoluteRelevance(1.0 / (neighbor.getValue() + 0.000000001));

            lastDistance = neighbor.getValue();
            ++ck;
        }

        return bestFitList;
    }

    /**
     * <p>
     * Fetches the possible {@link Categories} from a list of {@link Instance} like to ones making up the typical
     * training set.
     * </p>
     * 
     * @param instances The {@code List} of {@code NominalInstance}s to extract the {@code Categories} from.
     */
    private Categories getPossibleCategories(List<Instance> instances) {
        Categories categories = new Categories();
        for (Instance instance : instances) {
            Category category = new Category(instance.targetClass);
            category.increaseFrequency();
            categories.add(category);
        }
        categories.calculatePriors();
        return categories;
    }

    /**
     * <p>
     * Distance function, the shorter the distance the more important the category of the known instance. Euclidean
     * Distance = sqrt(SUM_0,n (i1-i2)²)
     * </p>
     * 
     * @param vector The instance to classify.
     * @param featureVector The instance in the vector space with known categories.
     * @return distance The Euclidean distance between the two instances in the vector space.
     */
    private double getDistanceBetween(FeatureVector vector, FeatureVector featureVector) {

        // XXX factor this distance measure out to a strategy class.

        double squaredSum = 0;

        List<NumericFeature> instanceFeatures = vector.getAll(NumericFeature.class);

        for (NumericFeature instanceFeature : instanceFeatures) {
            squaredSum += Math.pow(
                    instanceFeature.getValue()
                            - featureVector.getFeature(NumericFeature.class, instanceFeature.getName()).getValue(), 2);
        }

        return Math.sqrt(squaredSum);
    }

    @Override
    public KnnModel train(Dataset dataset) {
        // FIXME
        return null;
    }
}