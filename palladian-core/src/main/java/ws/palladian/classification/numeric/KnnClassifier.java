package ws.palladian.classification.numeric;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.Pair;

import ws.palladian.core.CategoryEntries;
import ws.palladian.core.CategoryEntriesBuilder;
import ws.palladian.core.Classifier;
import ws.palladian.core.FeatureVector;
import ws.palladian.core.Instance;
import ws.palladian.helper.collection.CollectionHelper.Order;
import ws.palladian.helper.collection.EntryValueComparator;

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
	 * Creates a new KNN classifier with specified k. A typical value for k is
	 * 3.
	 * </p>
	 * 
	 * @param k
	 *            The parameter k specifying the k nearest neighbors to use for
	 *            classification. Must be greater zero.
	 */
	public KnnClassifier(int k) {
		Validate.isTrue(k > 0, "k must be greater zero");
		this.k = k;
	}

	/**
	 * <p>
	 * Creates a new KNN classifier with a k of 3.
	 * </p>
	 */
	public KnnClassifier() {
		this(3);
	}

    @Override
    public CategoryEntries classify(FeatureVector featureVector, KnnModel model) {

        // initialize with all category names and a score of zero
        CategoryEntriesBuilder builder = new CategoryEntriesBuilder().set(model.getCategories(), 0);
        
        double[] numericVector = model.getNormalizedVectorForClassification(featureVector);

        // find k nearest neighbors, compare instance to every known instance
        List<Pair<String, Double>> neighbors = new ArrayList<>();
        for (TrainingExample example : model.getTrainingExamples()) {
            double distance = example.distance(numericVector);
            neighbors.add(Pair.of(example.category, distance));
        }

        // sort near neighbor map by distance
        Collections.sort(neighbors, new EntryValueComparator<Double>(Order.ASCENDING));

        // if there are several instances at the same distance we take all of them into the voting, k might get bigger
        // in those cases
        double lastDistance = -1;
        int ck = 0;
        for (Pair<String, Double> neighbor : neighbors) {
            double distance = neighbor.getValue();
			if (ck >= k && distance != lastDistance) {
                break;
            }
            double weight = 1.0 / (distance + 0.000000001);
            String targetClass = neighbor.getKey();
            builder.add(targetClass, weight);
            lastDistance = distance;
            ck++;
        }

        return builder.create();
    }
    
    @Override
    public String toString() {
    	return getClass().getSimpleName() + " (k=" + k + ")"; 
    }

}
