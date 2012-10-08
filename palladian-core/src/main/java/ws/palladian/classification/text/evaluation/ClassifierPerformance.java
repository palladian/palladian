package ws.palladian.classification.text.evaluation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import ws.palladian.classification.Categories;
import ws.palladian.classification.Category;
import ws.palladian.classification.CategoryEntry;
import ws.palladian.classification.ClassifierPerformanceResult;
import ws.palladian.classification.text.TestDocument;
import ws.palladian.classification.text.TextInstance;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;

/**
 * <p>
 * This class calculates scores for a given classifier such as precision, recall, and F1 on one given dataset.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class ClassifierPerformance implements Serializable {

    private static final long serialVersionUID = -7375053843995436850L;

    /** The classifier's categories. */
    private final Categories categories;

    /** The training documents. */
    private final List<TextInstance> trainingDocuments;

    /** The test documents that can be used to calculate recall, precision, and F-score. */
    private final List<TextInstance> testDocuments;

    /**
     * A list of pairs of [correct,threshold] where correct is 0 or 1 and the threshold is the threshold of the document
     * that was classified. From this list we can later calculate threshold analysis charts.
     */
    private List<Double[]> correctThresholds = null;

    /**
     * Create a new ClassifierPerformance for a given classifier.
     * 
     * @param classifier The classifier.
     */
    public ClassifierPerformance(Categories categories, List<TextInstance> trainingDocuments,
            List<TextInstance> testDocuments) {
        this.categories = categories;
        this.trainingDocuments = trainingDocuments;
        this.testDocuments = testDocuments;
    }

    /**
     * Get the number of correct classified documents in a given category.
     * 
     * @param category The category.
     * @return Number of correct classified documents in a given category.
     */
    public int getNumberOfCorrectClassifiedDocumentsInCategory(Category category) {
        int number = 0;

        for (TextInstance document : testDocuments) {
            TestDocument testDocument = (TestDocument) document;

            if (category.getClassType() == ClassificationTypeSetting.SINGLE) {
                if (document.getMainCategoryEntry().getCategory().getName().equals(category.getName())
                        && testDocument.isCorrectClassified()) {
                    ++number;
                }
            } else {
                for (CategoryEntry c : testDocument.getAssignedCategoryEntries()) {
                    if (c.getCategory().getName().equals(category.getName()) && testDocument.isCorrectClassified()) {
                        ++number;
                        break;
                    }
                }
            }

        }

        return number;
    }

    // FIXME this assumes cClassificationTypeSetting.SINGLE
    public int getNumberOfConfusionsBetween(Category actualCategory, Category classifiedCategory) {
        int number = 0;

        for (TextInstance document : testDocuments) {
            TestDocument testDocument = (TestDocument) document;

            if (testDocument.getFirstRealCategory().getName().equals(actualCategory.getName()) &&
                    document.getMainCategoryEntry().getCategory().getName().equals(classifiedCategory.getName())) {

                number++;

            }

        }

        return number;
    }

    public double getCorrectlyClassified() {
        int correctlyClassified = 0;
        for (Category c : categories) {
            correctlyClassified += getNumberOfCorrectClassifiedDocumentsInCategory(c);
        }

        return correctlyClassified / (double) testDocuments.size();
    }

    /**
     * <p>Get the prior of the most likely category. In a dataset with evenly distributed classes the highest prior should be 1/#classes.</p>
     * @return The highest prior.
     */
    public double getHighestPrior() {

        double highestPrior = -1.0;

        CountMap<String> countMap = CountMap.create();
        for (TextInstance document : testDocuments) {
            countMap.add(document.getFirstRealCategory().getName());
        }

        Integer highestClassCount = countMap.getSortedMapDescending().values().iterator().next();
        if (highestClassCount != null && highestClassCount > 0) {
            highestPrior = highestClassCount / (double) testDocuments.size();
        }

        return highestPrior;
    }

    /**
     * calculate and return the precision for a given category
     * 
     * @param category
     * @return the precision for a given category
     */
    public double getPrecisionForCategory(Category category) {
        try {
            double correct = getNumberOfCorrectClassifiedDocumentsInCategory(category);
            double classified = getClassifiedNumberOfCategory(testDocuments, category);

            if (classified < 1.0) {
                return -1.0;
            }

            return correct / classified;
        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Precision in Category " + category.getName());
            return -1.0;
        }
    }

    /**
     * calculate and return the recall for a given category
     * 
     * @param category
     * @return the recall for a given category
     */
    public double getRecallForCategory(Category category) {
        try {
            double correct = getNumberOfCorrectClassifiedDocumentsInCategory(category);
            double real = getRealNumberOfCategory(testDocuments, category);
            if (real < 1.0) {
                return -1.0;
            }
            return correct / real;
        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Recall in Category " + category.getName());
            return -1.0;
        }
    }

    /**
     * Calculate and return the F for a given category.
     * 
     * @param category The category.
     * @param alpha A value between 0 and 1 to weight precision and recall (0.5 for F1).
     * @return F for a given category.
     */
    public double getFForCategory(Category category, double alpha) {
        try {
            double pfc = getPrecisionForCategory(category);
            double rfc = getRecallForCategory(category);

            if (pfc < 0 || rfc < 0) {
                return -1.0;
            }

            return 1.0 / (alpha * 1.0 / pfc + (1.0 - alpha) * 1.0 / rfc);
            // double alphaSquared = alpha * alpha;
            // return ((1+alphaSquared)*getPrecisionForCategory(category) * getRecallForCategory(category)) /
            // (alphaSquared*getPrecisionForCategory(category)+getRecallForCategory(category));
        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for F in Category " + category.getName());
            return -1.0;
        }
    }

    /**
     * Calculate the sensitivity for a given category. Sensitivity = TP / (TP + FN). Sensitivity specifies what
     * percentage of actual category members were
     * found. 100% sensitivity means that all actual documents belonging to the category were classified correctly.
     * 
     * @param category
     * @return
     */
    public double getSensitivityForCategory(Category category) {
        double sensitivity = 0.0;
        try {
            double truePositives = getNumberOfCorrectClassifiedDocumentsInCategory(category);
            double realPositives = getRealNumberOfCategory(testDocuments, category);

            double falseNegatives = realPositives - truePositives;

            if (truePositives + falseNegatives == 0) {
                return -1.0;
            }

            sensitivity = truePositives / (truePositives + falseNegatives);

        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Sensitivity in Category " + category.getName());
            return -1.0;
        }

        return sensitivity;
    }

    /**
     * Calculate the specificity for a given category. Specificity = (TN) / (TN + FP). Specificity specifies what
     * percentage of not-category members were
     * recognized as such. 100% specificity means that there were no documents classified as category member when they
     * were actually not.
     * 
     * @param category The category.
     * @return The specificity.
     */
    public double getSpecificityForCategory(Category category) {
        double specificity = 0.0;
        try {
            double truePositives = getNumberOfCorrectClassifiedDocumentsInCategory(category);
            double realPositives = getRealNumberOfCategory(testDocuments, category);
            double classifiedPositives = getClassifiedNumberOfCategory(testDocuments, category);

            double falsePositives = classifiedPositives - truePositives;
            double falseNegatives = realPositives - truePositives;
            double trueNegatives = testDocuments.size() - classifiedPositives - falseNegatives;

            if (trueNegatives + falsePositives == 0) {
                return -1.0;
            }

            specificity = trueNegatives / (trueNegatives + falsePositives);

        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Specificity in Category " + category.getName());
            return -1.0;
        }

        return specificity;
    }

    /**
     * Calculate the accuracy for a given category. Accuracy = (TP + TN) / (TP + TN + FP + FN).
     * 
     * @param category The category.
     * @return The accuracy.
     */
    public double getAccuracyForCategory(Category category) {
        double accuracy = 0.0;
        try {
            double truePositives = getNumberOfCorrectClassifiedDocumentsInCategory(category);
            double realPositives = getRealNumberOfCategory(testDocuments, category);
            double classifiedPositives = getClassifiedNumberOfCategory(testDocuments, category);

            double falsePositives = classifiedPositives - truePositives;
            double falseNegatives = realPositives - truePositives;
            double trueNegatives = testDocuments.size() - classifiedPositives - falseNegatives;

            if (truePositives + trueNegatives + falsePositives + falseNegatives == 0) {
                return -1.0;
            }

            accuracy = (truePositives + trueNegatives)
                    / (truePositives + trueNegatives + falsePositives + falseNegatives);

        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Accuracy in Category " + category.getName());
            return -1.0;
        }

        return accuracy;
    }

    /**
     * Calculate the prior for the given category. The prior is determined by calculating the frequency of the category
     * in the training and test set and
     * dividing it by the total number of documents. XXX use only test documents to determine prior?
     * 
     * @param category The category for which the prior should be determined.
     * @return The prior for the category.
     */
    public double getWeightForCategory(Category category) {
        if (category.getTestSetWeight() > -1) {
            return category.getTestSetWeight();
        }

        try {
            // the number of documents that belong to the given category
            int documentCount = getRealNumberOfCategory(testDocuments, category)
                    + getRealNumberOfCategory(trainingDocuments, category);

            // the total number of documents assigned to categories, one document can be assigned to multiple
            // categories!
            int totalAssigned = 0;
            for (Category c : categories) {

                totalAssigned += getRealNumberOfCategory(testDocuments, c)
                        + getRealNumberOfCategory(trainingDocuments, c);
            }

            // double ratio = (double) documentCount / (double) (testDocuments.size() + trainingDocuments.size());
            double weight = (double) documentCount / (double) totalAssigned;
            category.setTestSetWeight(weight);
            return weight;

        } catch (ArithmeticException e) {
            Logger.getRootLogger().error("ERROR Division By Zero for Recall in Category " + category.getName());
            return 0.0;
        }
    }

    /**
     * Get the average precision of all categories.
     * 
     * @return The average precision of all categories.
     */
    public double getAveragePrecision(boolean weighted) {
        double precision = 0.0;

        double count = 0.0;


            for (Category c : categories) {


                double pfc = getPrecisionForCategory(c);
                if (pfc < 0) {
                    continue;
                }

                if (weighted) {
                    precision += getWeightForCategory(c) * pfc;
                } else {
                    precision += pfc;
                }
                ++count;
            }

            if (weighted) {
                return precision;
            }



        if (count == 0) {
            return -1.0;
        }

        return precision / count;
    }

    /**
     * Get the average recall of all categories.
     * 
     * @return The average recall of all categories.
     */
    public double getAverageRecall(boolean weighted) {
        double recall = 0.0;

        double count = 0.0;

            for (Category c : categories) {

                double rfc = getRecallForCategory(c);
                if (rfc < 0.0) {
                    continue;
                }

                if (weighted) {
                    recall += getWeightForCategory(c) * rfc;
                } else {
                    recall += rfc;
                }
                ++count;
            }

            if (weighted) {
                return recall;
            }


        if (count == 0) {
            return -1.0;
        }

        return recall / count;
    }

    /**
     * Get the average F of all categories.
     * 
     * @param alpha To weight precision and recall (0.5 for F1).
     * @return The average F of all categories.
     */
    public double getAverageF(double alpha, boolean weighted) {
        double f = 0.0;

        double count = 0.0;
        for (Category c : categories) {

            double ffc = getFForCategory(c, alpha);

            if (ffc < 0.0) {
                continue;
            }

            if (weighted) {
                f += getWeightForCategory(c) * ffc;
            } else {
                f += ffc;
            }

            ++count;
        }

        if (weighted) {
            return f;
        }

        if (count == 0) {
            return -1.0;
        }

        return f / count;
    }

    /**
     * Calculate the average sensitivity.
     * 
     * @param weighted If true, the average sensitivity is weighted using the priors of the categories.
     * @return The (weighted) average sensitivity.
     */
    public double getAverageSensitivity(boolean weighted) {
        double sensitivity = 0.0;

        double count = 0.0;
        for (Category c : categories) {

            double sfc = getSensitivityForCategory(c);

            if (sfc < 0.0) {
                continue;
            }

            if (weighted) {
                sensitivity += getWeightForCategory(c) * sfc;
            } else {
                sensitivity += sfc;
            }

            ++count;
        }

        if (weighted) {
            return sensitivity;
        }

        if (count == 0) {
            return -1.0;
        }

        return sensitivity / count;
    }

    /**
     * Calculate the average specificity.
     * 
     * @param weighted If true, the average accuracy is weighted using the priors of the categories.
     * @return The (weighted) average accuracy.
     */
    public double getAverageSpecificity(boolean weighted) {
        double specificity = 0.0;

        double count = 0.0;
        for (Category c : categories) {

            double sfc = getSpecificityForCategory(c);

            if (sfc < 0) {
                return -1.0;
            }

            if (weighted) {
                specificity += getWeightForCategory(c) * sfc;
            } else {
                specificity += sfc;
            }

            ++count;
        }

        if (weighted) {
            return specificity;
        }

        if (count == 0) {
            return -1.0;
        }

        return specificity / count;
    }

    /**
     * Calculate the average accuracy.
     * 
     * @param weighted If true, the average accuracy is weighted using the priors of the categories.
     * @return The (weighted) average accuracy.
     */
    public double getAverageAccuracy(boolean weighted) {
        double accuracy = 0.0;

        double count = 0.0;
        for (Category c : categories) {

            double afc = getAccuracyForCategory(c);

            if (afc < 0.0) {
                return -1.0;
            }

            if (weighted) {
                accuracy += getWeightForCategory(c) * afc;
            } else {
                accuracy += afc;
            }

            ++count;
        }

        if (weighted) {
            return accuracy;
        }

        if (count == 0) {
            return -1.0;
        }

        return accuracy / count;
    }

    /**
     * <p>
     * Calculate a confusion matrix.
     * </p>
     * <p>
     * One such matrix could look like this:
     * </p>
     * 
     * <pre>
     * classified\actual    | mobile phone  | camera | tv set
     * ----------------------------------------------------------
     * mobile phone         | 10            | 11     | 3
     * camera               | 4             | 20     | 5
     * tv set               | 1             | 1      | 31
     * </pre>
     * 
     * @return The confusion matrix.
     */
    public ConfusionMatrix getConfusionMatrix() {

        // x = actual category, y = classified category
        ConfusionMatrix confusionMatrix = new ConfusionMatrix();

        for (Category actualCategory : categories) {

            for (Category classifiedCategory : categories) {

                int count = getNumberOfConfusionsBetween(actualCategory, classifiedCategory);

                confusionMatrix.set(actualCategory.getName(), classifiedCategory.getName(), count);
            }

        }

        return confusionMatrix;
    }

    /**
     * Write the most basic scores to a result class which only holds these but not the documents. The
     * ClassifierPerformanceResult class saves memory.
     * 
     * @return The most basic results held in the {@link ClassifierPerformanceResult} class.
     */
    public ClassifierPerformanceResult getClassifierPerformanceResult() {

        ClassifierPerformanceResult classifierPerformanceResult = new ClassifierPerformanceResult();

        classifierPerformanceResult.setPrecision(getAveragePrecision(true));
        classifierPerformanceResult.setRecall(getAverageRecall(true));
        classifierPerformanceResult.setF1(getAverageF(0.5, true));

        classifierPerformanceResult.setSensitivity(getAverageSensitivity(true));
        classifierPerformanceResult.setSpecificity(getAverageSpecificity(true));
        classifierPerformanceResult.setAccuracy(getAverageAccuracy(true));

        classifierPerformanceResult.setCorrectlyClassified(getCorrectlyClassified());

        classifierPerformanceResult.setSuperiority(getCorrectlyClassified() / getHighestPrior());

        classifierPerformanceResult.setConfusionMatrix(getConfusionMatrix());

        classifierPerformanceResult.setThresholdBucketMap(getThresholdBucketMap());
        classifierPerformanceResult.setThresholdAccumulativeMap(getThresholdAccumulativeMap());

        return classifierPerformanceResult;
    }

    private List<Double[]> getCorrectThresholds() {

        if (correctThresholds == null) {

            correctThresholds = new ArrayList<Double[]>();

            for (TextInstance document : testDocuments) {
                TestDocument testDocument = (TestDocument)document;

                // pair containing correct (0 or 1) and the threshold [0,1]
                Double[] pair = new Double[2];

                pair[0] = 0.0;
                if (testDocument.isCorrectClassified()) {
                    pair[0] = 1.0;
                }

                pair[1] = testDocument.getMainCategoryEntry().getTrust();

                correctThresholds.add(pair);
            }

        }

        return correctThresholds;
    }

    /**
     * <p>
     * Get a map holding the thresholds in .01 steps with the correctlyClassified value of all documents with a
     * threshold >= the threshold.
     * </p>
     * 
     * <pre>
     * threshold    | correctly classified % | number of documents >= threshold
     * -------------|------------------------|---------------------------------
     * 0.01         |                        |
     * ...          |                        |
     * 1.00         |                        |
     * </pre>
     * 
     * @return A map with 101 entries (0.00 - 1.00) of thresholds, the percentage ofdocuments that were classified
     *         correctly having this or a greater threshold and the number of documents greater or equal the threshold.
     */
    private Map<Double, Double[]> getThresholdAccumulativeMap() {

        TreeMap<Double, Double[]> map = new TreeMap<Double, Double[]>();

        List<Double[]> ct = getCorrectThresholds();

        for (double t = 0.00; t <= 1.00; t += 0.01) {
            int correctlyClassified = 0;
            int numberOverThreshold = 0;

            for (Double[] doubles : ct) {

                if (doubles[1] >= t) {
                    numberOverThreshold++;
                    if (doubles[0] > 0.0) {
                        correctlyClassified++;
                    }
                }

            }

            Double[] entry = new Double[2];
            entry[0] = correctlyClassified / (double)numberOverThreshold;
            entry[1] = (double)numberOverThreshold;
            map.put(t, entry);
        }

        return map;
    }

    /**
     * <p>
     * Get a map holding the thresholds in buckets 0-0.1,0.1-0.2... with the buckets correctlyClassified. We will be
     * able to see how correct classified items are when having a trust within the bucket's threshold.
     * </p>
     * 
     * <pre>
     * threshold bucket | correctly classified % | number of documents in the bucket
     * -----------------|------------------------|---------------------------------
     * 0.1-0.2          |                        |
     * ...              |                        |
     * 0.9-1.00         |                        |
     * </pre>
     * 
     * @return A map with threshold buckets, the correctly classified documents in the bucket and the total number of
     *         documents in the bucket.
     */
    private Map<Double, Double[]> getThresholdBucketMap() {

        TreeMap<Double, Double[]> map = new TreeMap<Double, Double[]>();

        List<Double[]> ct = getCorrectThresholds();

        for (double t = 0.00; t <= 0.90; t += 0.1) {
            int correctlyClassified = 0;
            int numberInBucket = 0;

            for (Double[] doubles : ct) {

                if (doubles[1] > t && doubles[1] <= t + 0.1) {
                    numberInBucket++;
                    if (doubles[0] > 0.0) {
                        correctlyClassified++;
                    }
                }

            }

            Double[] entry = new Double[2];
            entry[0] = correctlyClassified / (double)numberInBucket;
            entry[1] = (double)numberInBucket;
            map.put(t, entry);
        }

        return map;

    }

    public void save(String evaluationOutputPath) {
        StringBuilder results = new StringBuilder();

        ClassifierPerformanceResult cpr = getClassifierPerformanceResult();

        // get the main measurements such as precision, recall, and F1
        results.append(toReadableString());
        results.append("\n\n");

        // add the confusion matrix
        results.append("confusion matrix\n");
        results.append(cpr.getConfusionMatrix().asCsv());
        results.append("\n\n");

        // add data for threshold analysis
        results.append(cpr.getThresholdBucketMapAsCsv());
        results.append("\n\n");

        results.append(cpr.getThresholdAccumulativeMapAsCsv());
        results.append("\n\n");

        results.append("\nAll scores are averaged over all classes in the dataset and weighted by their priors.");
        FileHelper.writeToFile(evaluationOutputPath, results);
    }

    public String toReadableString() {

        ClassifierPerformanceResult cpr = getClassifierPerformanceResult();

        StringBuilder builder = new StringBuilder();
        builder.append("Precision: ").append(cpr.getPrecision()).append("\n");
        builder.append("Recall: ").append(cpr.getRecall()).append("\n");
        builder.append("F1: ").append(cpr.getF1()).append("\n");
        builder.append("Sensitivity: ").append(cpr.getSensitivity()).append("\n");
        builder.append("Specificity: ").append(cpr.getSpecificity()).append("\n");
        builder.append("Accuracy: ").append(cpr.getAccuracy()).append("\n");
        builder.append("Correctly Classified: ").append(cpr.getCorrectlyClassified()).append("\n");
        builder.append("Superiority: ").append(cpr.getSuperiority()).append("\n");
        return builder.toString();

    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("ClassifierPerformance [");
        builder.append(getClassifierPerformanceResult());
        builder.append("]");
        return builder.toString();
    }
    
    /**
     * Get the number of documents that have been assigned to given category.
     * 
     * @param categoryName The category.
     * @return number The number of documents classified in the given category.
     */
    private int getClassifiedNumberOfCategory(List<TextInstance> instances, Category category) {
        int number = 0;


            for (TextInstance d : instances) {
                if (d.getMainCategoryEntry().getCategory().getName().equals(category.getName())) {
                    ++number;
                }
            }


        return number;
    }
    
    /**
     * Get the number of documents that actually ARE in the given category.
     * 
     * @param category
     * @return number
     */
    public int getRealNumberOfCategory(List<TextInstance> instances, Category category) {
        int number = 0;

        for (TextInstance d : instances) {
            for (Category c : d.getRealCategories()) {
                if (c.getName().equals(category.getName())) {
                    ++number;
                }
            }
        }

        return number;
    }

}
