package ws.palladian.classification.numeric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instance;
import ws.palladian.classification.Instances;
import ws.palladian.classification.Model;
import ws.palladian.classification.NominalInstance;
import ws.palladian.classification.Predictor;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Feature;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * A concrete KNN (K - Nearest Neighbour) classifier. I classifies
 * {@link FeatureVector}s based on the k nearest {@link Instance}s from the
 * training set.
 * </p>
 * <p>
 * Since this is an instance based classifier it is fast during the learning
 * phase but has a more complicated prediction phase.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 */
public class KnnClassifier implements Predictor<KnnModel> {

	/**
	 * Number of nearest neighbors that are allowed to vote. If neighbors have
	 * the same distance they will all be considered for voting, k might
	 * increase in these cases.
	 */
	private int k = 3;

	/**
	 * <p>
	 * Creates a new completely initialized KNN classifier with specified k. A
	 * typical value is 3. This constructor should be used if the created object
	 * is used for prediction.
	 * </p>
	 * 
	 * @param k
	 *            The parameter k specifying the k nearest neighbours to use for
	 *            classification.
	 */
	public KnnClassifier(Integer k) {
		super();

		this.k = k;
	}

	/**
	 * <p>
	 * Creates a new completely initialized KNN classifier with a k of 3. This
	 * constructor should typically be used if the class is used for learning.
	 * In that case the value of k is not important. It is only used during
	 * prediction.
	 * </p>
	 */
	public KnnClassifier() {
		super();

		this.k = 3;
	}

	@Override
	public KnnModel learn(List<NominalInstance> instances) {
		List<NominalInstance> normalizedInstances = normalize(instances);
		KnnModel model = new KnnModel(normalizedInstances);
		return model;
	}

	private List<NominalInstance> normalize(List<NominalInstance> instances) {
        // hold the min value of each feature <featureIndex, minValue>
        Map<Integer, Double> featureMinValueMap = new HashMap<Integer, Double>();

        // hold the max value of each feature <featureIndex, maxValue>
        Map<Integer, Double> featureMaxValueMap = new HashMap<Integer, Double>();

        // find the min and max values
        for (NominalInstance instance :instances) {

//            UniversalInstance nInstance = (UniversalInstance)instance;
            List<Feature<Double>> numericFeatures = instance.featureVector.getAll(Double.class);

            for (int i = 0; i < numericFeatures.size(); i++) {

                double featureValue = numericFeatures.get(i).getValue();

                // check min value
                if (featureMinValueMap.get(i) != null) {
                    double currentMin = featureMinValueMap.get(i);
                    if (currentMin > featureValue) {
                        featureMinValueMap.put(i, featureValue);
                    }
                } else {
                    featureMinValueMap.put(i, featureValue);
                }

                // check max value
                if (featureMaxValueMap.get(i) != null) {
                    double currentMax = featureMaxValueMap.get(i);
                    if (currentMax < featureValue) {
                        featureMaxValueMap.put(i, featureValue);
                    }
                } else {
                    featureMaxValueMap.put(i, featureValue);
                }

            }
        }

        // normalize the feature values
        MinMaxNormalization minMaxNormalization = new MinMaxNormalization();
        Map<Integer, Double> normalizationMap = new HashMap<Integer, Double>();
        List<NominalInstance> normalizedInstances = new ArrayList<NominalInstance>(instances.size());
        for (NominalInstance instance : instances) {
        	NominalInstance normalizedInstance = new NominalInstance();
        	normalizedInstance.target = instance.target;
        	
//            UniversalInstance nInstance = (UniversalInstance)instance;
            List<Feature<Double>> numericFeatures = instance.featureVector.getAll(Double.class);

            for (int i = 0; i < numericFeatures.size(); i++) {

                double max_minus_min = featureMaxValueMap.get(i) - featureMinValueMap.get(i);
                Feature<Double> currentFeature = numericFeatures.get(i);
				double featureValue = currentFeature.getValue();
                double normalizedValue = (featureValue - featureMinValueMap.get(i)) / max_minus_min;

                normalizedInstance.featureVector = new FeatureVector();
                normalizedInstance.featureVector.add(new NumericFeature(FeatureDescriptorBuilder.build(currentFeature.getName(), NumericFeature.class), normalizedValue));

                normalizationMap.put(i, max_minus_min);
                minMaxNormalization.getMinValueMap().put(i, featureMinValueMap.get(i));
            }

        }

        minMaxNormalization.setNormalizationMap(normalizationMap);
        return normalizedInstances;
	}

	/**
	 * Classify a given {@link FeatureVector} using the provided
	 * {@link KnnModel}.
	 * 
	 * @param instance
	 *            The instance to be classified.
	 */
	@Override
	public CategoryEntries predict(FeatureVector vector, KnnModel model) {

		// StopWatch stopWatch = new StopWatch();

		// if (categories == null) {
		Categories categories = getPossibleCategories(model
				.getTrainingInstances());
		// }

//		// we need to normalize the new instance if the training instances were
//		// also normalized
//		if (getTrainingInstances().areNormalized()) {
//			instance.normalize(getTrainingInstances().getMinMaxNormalization());
//		}
//		List<NominalInstance> normalizedInstances = normalize(model.getTrainingInstances());

		CategoryEntries bestFitList = new CategoryEntries();

		// create one category entry for every category with relevance 0
		for (Category category : categories) {
			CategoryEntry c = new CategoryEntry(bestFitList, category, 0);
			bestFitList.add(c);
		}

		// find k nearest neighbors, compare instance to every known instance
		Map<String, Double> neighbors = new HashMap<String, Double>();
		for (NominalInstance knownInstance : model.getTrainingInstances()) {
			double distance = getDistanceBetween(vector,
					knownInstance.featureVector);
			neighbors.put(knownInstance.target, distance);
		}

		// CollectionHelper.print(neighbors, 10);

		// sort near neighbor map by distance
		Map<String, Double> sortedList = CollectionHelper
				.sortByValue(neighbors);

		// CollectionHelper.print(sortedList, 10);

		// get votes from k nearest neighbors and decide in which category the
		// document is in. Also consider distance for nearest neighbors
		Map<String, Double> votes = new HashMap<String, Double>();
		int ck = 0;

		// if there are several instances at the same distance we take all of
		// them into the voting, k might get bigger
		// in those cases
		double lastDistance = -1;
		for (Entry<String, Double> entry : sortedList.entrySet()) {

			if (ck >= k && entry.getValue() != lastDistance) {
				break;
			}

			String categoryName = entry.getKey();
			if (votes.containsKey(categoryName)) {
				votes.put(categoryName,
						votes.get(categoryName) + 1.0
								/ (entry.getValue() + 0.000000001));
			} else {
				votes.put(categoryName, 1.0 / (entry.getValue() + 0.000000001));
			}

			lastDistance = entry.getValue();
			++ck;
		}

		LinkedHashMap<String, Double> sortedVotes = CollectionHelper
				.sortByValue(votes, CollectionHelper.DESCENDING);

		// assign category entries
		for (Entry<String, Double> entry : sortedVotes.entrySet()) {

			CategoryEntry c = bestFitList.getCategoryEntry(entry.getKey());
			if (c == null) {
				continue;
			}

			c.addAbsoluteRelevance(entry.getValue());
		}

		return bestFitList;
	}

	/**
	 * <p>
	 * Fetches the possible {@link Categories} from a list of {@link NominalInstance} like to ones making up the typical training set.
	 * </p>
	 * 
	 * @param instances The {@code List} of {@code NominalInstance}s to extract the {@code Categories} from.
	 */
	protected Categories getPossibleCategories(List<NominalInstance> instances) {
		Categories categories = new Categories();
		for (NominalInstance instance : instances) {
			categories.add(new Category(instance.target));
		}
		categories.calculatePriors();
		return categories;
	}

	/**
	 * <p>
	 * Distance function, the shorter the distance the more important the
	 * category of the known instance. Euclidean Distance = sqrt(SUM_0,n
	 * (i1-i2)²)
	 * </p>
	 * 
	 * @param vector
	 *            The instancne to classify.
	 * @param featureVector
	 *            The instance in the vector space with known categories.
	 * @return distance The Euclidean distance between the two instances in the
	 *         vector space.
	 */
	private Double getDistanceBetween(FeatureVector vector,
			FeatureVector featureVector) {

		double distance = Double.MAX_VALUE;

		double squaredSum = 0;

		List<Feature<Double>> instanceFeatures = vector.getAll(Double.class);
		List<Feature<Double>> knownInstanceFeatures = vector
				.getAll(Double.class);

		for (int i = 0; i < instanceFeatures.size(); i++) {
			squaredSum += Math.pow(instanceFeatures.get(i).getValue()
					- knownInstanceFeatures.get(i).getValue(), 2);
		}

		distance = Math.sqrt(squaredSum);

		return distance;
	}
}