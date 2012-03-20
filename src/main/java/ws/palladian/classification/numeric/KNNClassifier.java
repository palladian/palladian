package ws.palladian.classification.numeric;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntries;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.Instances;
import ws.palladian.classification.page.evaluation.ClassificationTypeSetting;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;

/**
 * A concrete KNN classifier.
 * 
 * @author David Urbansky
 */
public final class KNNClassifier extends NumericClassifier {

    private static final long serialVersionUID = 1064061946261174688L;

    /** Non-transient training instances. We need to save them as the instance based classifier depends on them. */
    private Instances<NumericInstance> trainingInstances = new Instances<NumericInstance>();

    /**
     * Number of nearest neighbors that are allowed to vote. If neighbors have the same distance they will all be
     * considered for voting, k might increase in these cases.
     */
    private int k = 3;

    /** Non-transient training instances. We need to save them as the instance based classifier depends on them. */
    // private Instances<NumericInstance> serializableTrainingInstances;


    /**
     * The constructor.
     */
    public KNNClassifier() {
        setName("k-NN");
    }

    @Override
    public void classify(Instances<NumericInstance> instances) {
        for (NumericInstance instance : instances) {
            classify(instance);
        }
    }

    @Override
    /**
     * Classify a given instance.
     * @param instance The instance to be classified.
     */
    public void classify(NumericInstance instance) {

        StopWatch stopWatch = new StopWatch();

        if (categories == null) {
            getPossibleCategories(getTrainingInstances());
        }

        // we need to normalize the new instance if the training instances were also normalized
        if (getTrainingInstances().areNormalized()) {
            instance.normalize(getTrainingInstances().getMinMaxNormalization());
        }

        int classType = getClassificationType();

        // make a look up in the context map for every single term
        CategoryEntries bestFitList = new CategoryEntries();

        // create one category entry for every category with relevance 0
        for (Category category : getCategories()) {
            CategoryEntry c = new CategoryEntry(bestFitList, category, 0);
            bestFitList.add(c);
        }

        // find k nearest neighbors, compare instance to every known instance
        Map<NumericInstance, Double> neighbors = new HashMap<NumericInstance, Double>();
        for (NumericInstance knownInstance : getTrainingInstances()) {
            double distance = getDistanceBetween(instance, knownInstance);
            neighbors.put(knownInstance, distance);
        }

        // CollectionHelper.print(neighbors, 10);

        // sort near neighbor map by distance
        Map<NumericInstance, Double> sortedList = CollectionHelper.sortByValue(neighbors);

        // CollectionHelper.print(sortedList, 10);

        // get votes from k nearest neighbors and decide in which category the document is in also consider distance for nearest neighbors
        Map<String, Double> votes = new HashMap<String, Double>();
        int ck = 0;

        // if there are several instances at the same distance we take all of them into the voting, k might get bigger
        // in those cases
        double lastDistance = -1;
        for (Entry<NumericInstance, Double> entry : sortedList.entrySet()) {

            if (ck >= k && entry.getValue() != lastDistance) {
                break;
            }

            NumericInstance votingDocument = entry.getKey();

            Category realCategory = votingDocument.getInstanceCategory();

            if (votes.containsKey(realCategory.getName())) {
                votes.put(realCategory.getName(), votes.get(realCategory.getName()) + 1.0
                        / (entry.getValue() + 0.000000001));
            } else {
                votes.put(realCategory.getName(), 1.0 / (entry.getValue() + 0.000000001));
            }

            lastDistance = entry.getValue();
            ++ck;
        }

        LinkedHashMap<String, Double> sortedVotes = CollectionHelper.sortByValue(votes, CollectionHelper.DESCENDING);

        // assign category entries
        for (Entry<String, Double> entry : sortedVotes.entrySet()) {

            CategoryEntry c = bestFitList.getCategoryEntry(entry.getKey());
            if (c == null) {
                continue;
            }

            c.addAbsoluteRelevance(entry.getValue());
        }

        instance.assignCategoryEntries(bestFitList);

        // keep only top X categories for tagging mode
        if (classType == ClassificationTypeSetting.TAG) {
            instance.limitCategories(classificationTypeSetting.getClassificationTypeTagSetting().getMinTags(),
                    classificationTypeSetting.getClassificationTypeTagSetting().getMaxTags(), classificationTypeSetting
                    .getClassificationTypeTagSetting().getTagConfidenceThreshold());
        }

        // keep only top category for single mode
        else if (classType == ClassificationTypeSetting.SINGLE) {
            instance.limitCategories(1, 1, 0.0);
        }

        if (instance.getAssignedCategoryEntries().isEmpty()) {
            Category unassignedCategory = new Category(null);
            getCategories().add(unassignedCategory);
            CategoryEntry defaultCE = new CategoryEntry(bestFitList, unassignedCategory, 1);
            instance.addCategoryEntry(defaultCE);
        }

        instance.setClassifiedAs(classType);

        LOGGER.debug("classified document (classType " + classType + ") in " + stopWatch.getElapsedTimeString() + " "
                + " ("
                + instance.getAssignedCategoryEntriesByRelevance(classType) + ")");
    }

    /**
     * Distance function, the shorter the distance the more important the category of the known instance.
     * Euclidean Distance = sqrt(SUM_0,n (i1-i2)²)
     * 
     * @param instance The instancne to classify.
     * @param knownInstance The instance in the vector space with known categories.
     * @return distance The Euclidean distance between the two instances in the vector space.
     */
    private Double getDistanceBetween(NumericInstance instance, NumericInstance knownInstance) {

        double distance = Double.MAX_VALUE;

        double squaredSum = 0;

        List<Double> instanceFeatures = instance.getFeatures();
        List<Double> knownInstanceFeatures = knownInstance.getFeatures();

        for (int i = 0; i < instance.getFeatures().size(); i++) {
            squaredSum += Math.pow(instanceFeatures.get(i) - knownInstanceFeatures.get(i), 2);
        }

        distance = Math.sqrt(squaredSum);

        return distance;
    }

    // public Instances<NumericInstance> getSerializableTrainingInstances() {
    // return serializableTrainingInstances;
    // }

    @Override
    public void save(String path) {
        // save the training instances since they are normally transient
        // serializableTrainingInstances = getTrainingInstances();
        FileHelper.serialize(this, path + getName() + ".gz");
    }

    public static KNNClassifier load(String classifierPath) {
        LOGGER.info("deserialzing classifier from " + classifierPath);

        KNNClassifier classifier = (KNNClassifier) FileHelper.deserialize(classifierPath);

        // we attach the serialized training instances
        // classifier.setTrainingInstances(classifier.serializableTrainingInstances);
        // classifier.serializableTrainingInstances = null;

        return classifier;
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

    @Override
    public Instances<NumericInstance> getTrainingInstances() {
        return trainingInstances;
    }

    @Override
    public void setTrainingInstances(Instances<NumericInstance> trainingInstances) {
        this.trainingInstances = trainingInstances;
        getPossibleCategories(trainingInstances);
    }

}