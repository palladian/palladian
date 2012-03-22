package ws.palladian.classification.page.evaluation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ws.palladian.classification.page.TextClassifier;

/**
 * The result of a cross validation for a classifier and given settings.
 * 
 * @author David Urbansky
 * 
 */
public class CrossValidationResult {

    /** The classifier for which the results are gathered. */
    private TextClassifier classifier;

    /**
     * The classification type settings used for the classifier, this needs to be copied and cannot be accessed from the
     * classifier itself since the settings might change for the same classifier.
     */
    private ClassificationTypeSetting classificationTypeSettings;

    /**
     * The feature settings used for the classifier, this needs to be copied and cannot be accessed from the classifier
     * itself since the settings might change for the same classifier.
     */
    private FeatureSetting featureSettings;

    /** Average performances over all datasets, training percentages, and folds. */
    private Set<ClassifierPerformance> performancesDatasetTrainingFolds = new HashSet<ClassifierPerformance>();

    /** Average performances over all training percentages and folds <Datasetname:performances> */
    private Map<String, HashSet<ClassifierPerformance>> performancesTrainingFolds = new HashMap<String, HashSet<ClassifierPerformance>>();

    /** Average performances over all folds <Datasetname,trainingpercentage:performances>. */
    private Map<String, HashSet<ClassifierPerformance>> performancesFolds = new HashMap<String, HashSet<ClassifierPerformance>>();

    public CrossValidationResult(TextClassifier classifier) {
        this.classifier = classifier;
        this.classificationTypeSettings = classifier.getClassificationTypeSetting();
        this.featureSettings = classifier.getFeatureSetting();
    }

    public void setClassifier(TextClassifier classifier) {
        this.classifier = classifier;
    }

    public TextClassifier getClassifier() {
        return classifier;
    }

    public void setClassificationTypeSetting(ClassificationTypeSetting classificationTypeSettings) {
        this.classificationTypeSettings = classificationTypeSettings;
    }

    public ClassificationTypeSetting getClassificationTypeSetting() {
        return classificationTypeSettings;
    }

    public void setFeatureSetting(FeatureSetting featureSettings) {
        this.featureSettings = featureSettings;
    }

    public FeatureSetting getFeatureSetting() {
        return featureSettings;
    }

    public Set<ClassifierPerformance> getPerformancesDatasetTrainingFolds() {
        return performancesDatasetTrainingFolds;
    }

    public void setPerformancesDatasetTrainingFolds(Set<ClassifierPerformance> performancesDatasetTrainingFolds) {
        this.performancesDatasetTrainingFolds = performancesDatasetTrainingFolds;
    }

    public Map<String, HashSet<ClassifierPerformance>> getPerformancesTrainingFolds() {
        return performancesTrainingFolds;
    }

    public void setPerformancesTrainingFolds(Map<String, HashSet<ClassifierPerformance>> performancesTrainingFolds) {
        this.performancesTrainingFolds = performancesTrainingFolds;
    }

    public Map<String, HashSet<ClassifierPerformance>> getPerformancesFolds() {
        return performancesFolds;
    }

    public void setPerformancesFolds(Map<String, HashSet<ClassifierPerformance>> performancesFolds) {
        this.performancesFolds = performancesFolds;
    }

    /**
     * Calculate the average classifier performance when all performances over all datasets, training percentages, and
     * folds are averaged.
     * 
     * @return An average classifier performance.
     */
    public AverageClassifierPerformance getAveragePerformanceDataSetTrainingFolds() {
        AverageClassifierPerformance averageCP = new AverageClassifierPerformance();

        double avgPrecision = 0;
        double avgRecall = 0;

        for (ClassifierPerformance cp : getPerformancesDatasetTrainingFolds()) {
            avgPrecision += cp.getAveragePrecision(false);
            avgRecall += cp.getAverageRecall(false);
        }

        avgPrecision /= getPerformancesDatasetTrainingFolds().size();
        avgRecall /= getPerformancesDatasetTrainingFolds().size();

        averageCP.setPrecision(avgPrecision);
        averageCP.setRecall(avgRecall);

        return averageCP;
    }

    /**
     * Calculate the average classifier performance when all performances over all training percentages and
     * folds are averaged.
     * 
     * @return The average classifier performance for each dataset.
     */
    public Map<String, AverageClassifierPerformance> getAveragePerformanceTrainingFolds() {

        Map<String, AverageClassifierPerformance> result = new HashMap<String, AverageClassifierPerformance>();

        AverageClassifierPerformance averageCP = new AverageClassifierPerformance();

        for (Entry<String, HashSet<ClassifierPerformance>> cpEntry : getPerformancesTrainingFolds().entrySet()) {

            double avgPrecision = 0;
            double avgRecall = 0;

            for (ClassifierPerformance cp : cpEntry.getValue()) {
                avgPrecision += cp.getAveragePrecision(false);
                avgRecall += cp.getAverageRecall(false);
            }

            avgPrecision /= cpEntry.getValue().size();
            avgRecall /= cpEntry.getValue().size();

            averageCP.setPrecision(avgPrecision);
            averageCP.setRecall(avgRecall);

            result.put(cpEntry.getKey(), averageCP);

        }

        return result;
    }

    /**
     * Calculate the average classifier performance when all performances over all folds are averaged.
     * 
     * @return The average classifier performance for each dataset and training percentage.
     */
    public Map<String, AverageClassifierPerformance> getAveragePerformanceFolds() {

        Map<String, AverageClassifierPerformance> result = new HashMap<String, AverageClassifierPerformance>();

        AverageClassifierPerformance averageCP = new AverageClassifierPerformance();

        for (Entry<String, HashSet<ClassifierPerformance>> cpEntry : getPerformancesFolds().entrySet()) {

            double avgPrecision = 0;
            double avgRecall = 0;

            for (ClassifierPerformance cp : cpEntry.getValue()) {
                avgPrecision += cp.getAveragePrecision(false);
                avgRecall += cp.getAverageRecall(false);
            }

            avgPrecision /= cpEntry.getValue().size();
            avgRecall /= cpEntry.getValue().size();

            averageCP.setPrecision(avgPrecision);
            averageCP.setRecall(avgRecall);

            result.put(cpEntry.getKey(), averageCP);

        }

        return result;
    }
}