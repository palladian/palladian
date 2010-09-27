package tud.iir.classification.page;

import java.io.Serializable;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import tud.iir.classification.Category;
import tud.iir.classification.CategoryEntries;
import tud.iir.classification.CategoryEntry;
import tud.iir.classification.Term;
import tud.iir.classification.page.evaluation.ClassificationTypeSetting;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.StopWatch;

/**
 * a concrete KNN classifier
 * 
 * @author David Urbansky
 */
public class KNNClassifier extends TextClassifier implements Serializable {

    private static final long serialVersionUID = 184534402554023419L;

    /** Number of nearest neighbors that are allowed to vote. */
    private int k = 3;

    /**
     * The constructor.
     */
    public KNNClassifier() {
        ClassifierManager.log("KNN Classifier created");
        setName("k-NN");
    }

    @Override
    /**
     * Classify a given document.
     * @param document the document being classified
     */
    public ClassificationDocument classify(ClassificationDocument document) {

        StopWatch stopWatch = new StopWatch();

        int classType = getClassificationType();

        // make a look up in the context map for every single term
        CategoryEntries bestFitList = new CategoryEntries();

        // create one category entry for every category with relevance 0
        for (Category category : categories) {
            CategoryEntry c = new CategoryEntry(bestFitList, category, 0);
            bestFitList.add(c);
        }

        // find k nearest neighbors, compare document to every trained document
        Map<ClassificationDocument, Double> neighbors = new HashMap<ClassificationDocument, Double>();
        for (ClassificationDocument trainingDocument : getTrainingDocuments()) {
            double distance = getDistanceBetween(trainingDocument, document);
            neighbors.put(trainingDocument, distance);
        }

        // sort near neighbor map by distance
        Map<ClassificationDocument, Double> sortedList = CollectionHelper.sortByValue(neighbors.entrySet());

        // get votes from k nearest neighbors and decide in which category the document is in also consider distance for nearest neighbors
        Map<String, Double> votes = new HashMap<String, Double>();
        int ck = 0;
        for (Entry<ClassificationDocument, Double> entry : sortedList.entrySet()) {
            ClassificationDocument votingDocument = entry.getKey();

            // all assigned categories from the voting document should have influence to outcome
            for (Category realCategory : votingDocument.getRealCategories()) {

                if (votes.containsKey(realCategory.getName())) {
                    votes.put(realCategory.getName(), votes.get(realCategory.getName()) + 1.0 / entry.getValue());
                } else {
                    votes.put(realCategory.getName(), 1.0 / entry.getValue());
                }

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

        document.assignCategoryEntries(bestFitList);

        // keep only top X categories for tagging mode
        if (classType == ClassificationTypeSetting.TAG) {
            document.limitCategories(classificationTypeSetting.getClassificationTypeTagSetting().getMinTags(),
                    classificationTypeSetting.getClassificationTypeTagSetting().getMaxTags(), classificationTypeSetting
                            .getClassificationTypeTagSetting().getTagConfidenceThreshold());
        }

        // keep only top category for single mode
        else if (classType == ClassificationTypeSetting.SINGLE) {
            document.limitCategories(1, 1, 0.0);
        }

        if (document.getAssignedCategoryEntries().isEmpty()) {
            Category unassignedCategory = new Category(null);
            categories.add(unassignedCategory);
            CategoryEntry defaultCE = new CategoryEntry(bestFitList, unassignedCategory, 1);
            document.addCategoryEntry(defaultCE);
        }

        document.setClassifiedAs(classType);

        ClassifierManager.log("classified document (classType " + classType + ") in " + stopWatch.getElapsedTimeString() + " " + " ("
                + document.getAssignedCategoryEntriesByRelevance(classType) + ")");

        return document;
    }

    /**
     * Distance function, the shorter the distance the more important the document.
     * 
     * @param classifyDocument The document to classify.
     * @param trainingDocument The document in the vector space with known categories.
     * @return distance The distance between the two documents in the vector space.
     */
    private Double getDistanceBetween(ClassificationDocument classifyDocument, ClassificationDocument trainingDocument) {

        double matches = 0.0001;

        Set<Map.Entry<Term, Double>> entrySet1 = classifyDocument.getWeightedTerms().entrySet();
        
        for (Entry<Term, Double> entry1 : entrySet1) {

            if (trainingDocument.getWeightedTerms().containsKey(entry1.getKey())) {

                // Double entry2Value = trainingDocument.getWeightedTerms().get(entry1.getKey());
                // double similarity = 1.0 / (Math.abs(entry1.getValue() - entry2Value) + 1.5);
                // double importance = Math.pow((entry1.getValue() + entry2Value) / 2.0, 2);
                // matches += similarity * importance;

                matches++;

            }

        }

        return 1.0 / matches;
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

    /**
     * Get parameters used for the classifier (only k).
     */
    @Override
    public String getParameters() {
        return "k = " + getK();
    }

    @Override
    public ClassificationDocument preprocessDocument(String url) {
        return preprocessor.preProcessDocument(url);
    }

    @Override
    public ClassificationDocument preprocessDocument(String url, ClassificationDocument classificationDocument) {
        return preprocessor.preProcessDocument(url, classificationDocument);
    }

}