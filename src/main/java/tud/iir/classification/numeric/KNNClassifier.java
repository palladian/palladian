package tud.iir.classification.numeric;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.page.ClassifierManager;
import tud.iir.classification.page.evaluation.ClassificationTypeSetting;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;

/**
 * A concrete KNN classifier.
 * 
 * @author David Urbansky
 */
public class KNNClassifier extends NumericClassifier {

    private static final long serialVersionUID = 1064061946261174688L;

    /** Number of nearest neighbors that are allowed to vote. */
    private int k = 3;

    /**
     * The constructor.
     */
    public KNNClassifier() {
        ClassifierManager.log("KNN Classifier created");
        setName("k-NN");
    }

    /**
     * Fill the vector space with known instances. The instances must be given in a CSV file in the following format:<br>
     * feature1;..;featureN;NominalClass<br>
     * All features must be real values and the class must be nominal. Each line is one training instance.
     */
    public void trainFromCSV(String trainingFilePath) {
        List<String> trainingLines = FileHelper.readFileToArray(trainingFilePath);

        NumericInstances trainingInstances = new NumericInstances();
        NumericInstance trainingInstance = null;
        List<Double> features = null;

        for (String trainingLine : trainingLines) {
            String[] parts = trainingLine.split(";");

            trainingInstance = new NumericInstance();
            features = new ArrayList<Double>();

            for (int f = 0; f < parts.length - 1; f++) {
                features.add(Double.valueOf(parts[f]));
            }

            trainingInstance.setFeatures(features);
            trainingInstance.setClassNominal(true);
            trainingInstance.setInstanceClass(parts[parts.length - 1]);
            trainingInstances.add(trainingInstance);
        }

        setTrainingInstances(trainingInstances);
    }

    @Override
    /**
     * Classify a given instance.
     * @param instance The instance being classified.
     */
    public void classify(NumericInstance instance) {

        StopWatch stopWatch = new StopWatch();

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

        // sort near neighbor map by distance
        Map<NumericInstance, Double> sortedList = CollectionHelper.sortByValue(neighbors.entrySet());

        // get votes from k nearest neighbors and decide in which category the document is in also consider distance for nearest neighbors
        Map<String, Double> votes = new HashMap<String, Double>();
        int ck = 0;
        for (Entry<NumericInstance, Double> entry : sortedList.entrySet()) {
            NumericInstance votingDocument = entry.getKey();

            Category realCategory = (Category) votingDocument.getInstanceClass();

            if (votes.containsKey(realCategory.getName())) {
                votes.put(realCategory.getName(), votes.get(realCategory.getName()) + 1.0
                        / (entry.getValue() + 0.000000001));
            } else {
                votes.put(realCategory.getName(), 1.0 / (entry.getValue() + 0.000000001));
            }

            if (ck == k) {
                break;
            }
            ++ck;
        }

        LinkedHashMap<String, Double> sortedVotes = CollectionHelper.sortByValue(votes.entrySet(), CollectionHelper.DESCENDING);

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
            categories.add(unassignedCategory);
            CategoryEntry defaultCE = new CategoryEntry(bestFitList, unassignedCategory, 1);
            instance.addCategoryEntry(defaultCE);
        }

        instance.setClassifiedAs(classType);

        ClassifierManager.log("classified document (classType " + classType + ") in " + stopWatch.getElapsedTimeString() + " " + " ("
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

    @Override
    public void save(String path) {
        FileHelper.serialize(this, path + getName() + ".ser");
    }

    public int getK() {
        return k;
    }

    public void setK(int k) {
        this.k = k;
    }

}