package tud.iir.classification.page.evaluation;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import tud.iir.classification.page.ClassifierManager;
import tud.iir.classification.page.TextClassifier;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;

/**
 * The CrossValidator valdidates a given classifier with the evaluation settings. It can also print results for manual
 * investigation.
 * 
 * @author David Urbansky
 * @author Sandro Reichert
 * 
 */
public class CrossValidator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(CrossValidator.class);

    /** The evaluation settings. */
    private EvaluationSetting evaluationSetting;

    /** The result of the cross validation is saved here too. */
    private CrossValidationResult cvResult;

    public void setEvaluationSetting(EvaluationSetting evaluationSetting) {
        this.evaluationSetting = evaluationSetting;
    }

    public EvaluationSetting getEvaluationSetting() {
        return evaluationSetting;
    }

    /**
     * Method to compare the open analytix performance for classification depending on values trainingPercentage,
     * threshold for assigning a second category and
     * number of loops to average the performance with fixed trainingPercentage and threshold but random select of lines
     * to be assigned to training and testing
     * set
     * 
     * @param trainingPercentageMin The percentage of the data set to be used for training - minimum value of loop,
     *            range [0,100].
     * @param trainingPercentageMax The percentage of the data set to be used for training - maximum value of loop,
     *            range [0,100].
     * @param trainingPercentageStep The percentage of the data set to be used for training - step between loops, range
     *            [0,100].
     * @param randomSplitTrainingDataSet If true, initial data set is split randomly into training and test set (fixed
     *            percentage but randomly chosen lines). If
     *            false, the first lines are training set and the remainder is the test set.
     * @param numberLoopsToAverage Number of loops to average the performance with fixed trainingPercentage and
     *            threshold but random select of lines to be
     *            assigned to training and testing set. Ignored if randomSplitTrainingDataSet=false, e.g. only one loop
     *            is executed per trainingPercentage and
     *            threshold.
     * @param thMin Minimum value for the threshold used to assign a second category.
     * @param thMax Maximum value for the threshold used to assign a second category.
     * @param thStep Value to add to the threshold per loop.
     * @param classType The type of WebPageClassifier to be used, e.g. WebPageClassifier.FIRST.
     */
    public CrossValidationResult crossValidate(TextClassifier classifier) {

        int kFolds = getEvaluationSetting().getkFolds();
        if (!getEvaluationSetting().isRandom()) {
            kFolds = 1;
        }

        cvResult = new CrossValidationResult(classifier);

        ClassifierManager classifierManager = new ClassifierManager();

        double trainingPercentageMin = getEvaluationSetting().getTrainingPercentageMin();
        double trainingPercentageMax = getEvaluationSetting().getTrainingPercentageMax();
        double trainingPercentageStep = getEvaluationSetting().getTrainingPercentageStep();

        // average performances over all datasets, training percentages and folds
        Set<ClassifierPerformance> performancesDatasetTrainingFolds = cvResult.getPerformancesDatasetTrainingFolds();

        // average performances over all training percentages and folds <Datasetname:performances>
        Map<String, HashSet<ClassifierPerformance>> performancesTrainingFolds = cvResult.getPerformancesTrainingFolds();

        // average performances over all folds <Datasetname,trainingpercentage:performances>
        Map<String, HashSet<ClassifierPerformance>> performancesFolds = cvResult.getPerformancesFolds();

        // iterate over all datasets
        for (Dataset dataset : getEvaluationSetting().getDatasets()) {

            int trainingPercentageLoop = 0;

            // keep a set of performances for all training percentages of the given dataset
            Set<ClassifierPerformance> performancesDatasetTrainingFoldsTemp0 = new HashSet<ClassifierPerformance>();

            // e.g. test from 40:60 to 90:10
            for (double trainingPercentage = trainingPercentageMin; trainingPercentage <= trainingPercentageMax; trainingPercentage += trainingPercentageStep) {

                LOGGER.info("\n start trainingPercentage classification loop on " + dataset.getPath()
                        + " with trainingPercentage "
                                + trainingPercentage + "%, random = " + getEvaluationSetting().isRandom() + "\n");

                // keep a set of performances for all folds of the given training percentage and dataset
                Set<ClassifierPerformance> performancesDatasetTrainingFoldsTemp1 = new HashSet<ClassifierPerformance>();

                // e.g. 10 loops to average over random selection training and test data
                for (int k = 0; k < kFolds; k++) {

                    LOGGER.info("\n start inner (cross-validation) classification loop on " + dataset.getPath()
                            + " with trainingPercentage "
                                    + trainingPercentage
                                    + "%, random = "
                                    + getEvaluationSetting().isRandom()
                                    + ", iteration = " + (k + 1) + "\n");

                    String currentTime = DateHelper.getCurrentDatetime();
                    new TrainingDataSeparation().separateFile(dataset.getPath(), "data/temp/cvDataTraining.csv",
                            "data/temp/cvDataTesting.csv", trainingPercentage, getEvaluationSetting().isRandom());


                    // classifierManager.setTrainingDataPercentage(100);
                    dataset.setPath("data/temp/cvDataTraining.csv");
                    classifierManager.trainClassifier(dataset, classifier);

                    // classifierManager.trainAndTestClassifier("data/temp/cvDataTraining.csv",
                    // WebPageClassifier.URL,classType, ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);

                    // classify
                    // classifierManager.setTrainingDataPercentage(0);
                    dataset.setPath("data/temp/cvDataTesting.csv");
                    classifierManager.testClassifier(dataset, classifier);

                    // classifierManager.trainAndTestClassifier("data/temp/cvDataTesting.csv", WebPageClassifier.URL,
                    // classType, ClassifierManager.CLASSIFICATION_TEST_MODEL);


                    // add the performance to the evaluation maps
                    performancesDatasetTrainingFolds.add(classifier.getPerformance());
                    performancesDatasetTrainingFoldsTemp1.add(classifier.getPerformance());

                    StringBuilder trainingsetPercentSB = new StringBuilder();
                    trainingsetPercentSB.append(currentTime);
                    trainingsetPercentSB.append("random trainingPercentage: ").append(trainingPercentage).append("\n");
                    FileHelper.appendToFile("data/temp/thresholds.txt", trainingsetPercentSB, false);

                }

                // add the performances to the evaluation maps
                performancesTrainingFolds.get(dataset.getPath() + "_" + trainingPercentage).addAll(
                        performancesDatasetTrainingFoldsTemp1);
                performancesDatasetTrainingFoldsTemp0.addAll(performancesDatasetTrainingFoldsTemp1);

                trainingPercentageLoop++;
            }

            performancesTrainingFolds.get(dataset.getPath()).addAll(performancesDatasetTrainingFoldsTemp0);
        }

        // output results
        StringBuilder finalResultSB = new StringBuilder();
        finalResultSB.append(getEvaluationSetting()).append(";\n");


        finalResultSB.append("\n");

        cvResult.setPerformancesDatasetTrainingFolds(performancesDatasetTrainingFolds);
        cvResult.setPerformancesTrainingFolds(performancesTrainingFolds);
        cvResult.setPerformancesFolds(performancesFolds);

        return cvResult;

        // for (int i = 0; i < numberTrainingPercentageLoops; i++) {
        // finalResultSB.append("train ").append(trainingPercentageUsed[i]).append("% ").append(useRandom).append(";");
        //
        // double culmulatedPrecision = 0;
        // for (int k = 0; k < kFolds; k++) {
        // culmulatedPrecision += openAnalytixPerformances[i][k];
        // }
        // finalResultSB.append(MathHelper.round(culmulatedPrecision / kFolds, 4)).append(";");
        //
        // finalResultSB.append("\n");
        // System.out.print("\n");
        // }
        //
        // FileHelper.writeToFile(resultFilePath, finalResultSB);
    }

    /**
     * Print the evaluation files where a user can find out which classifier under which settings is the best for the
     * given datasets.
     * Three files will be written, one where each classifier's performance will be averaged over all datasets, training
     * percentages, and folds. Another one where each classifier is only averaged over all training percentages and
     * folds and a last one where each classifier is only averaged over all folds for a given dataset and training
     * percentage.
     * 
     * @param cvResults A set of cross validation results.
     * @param outputFolder The path to the folder where the evaluation files should be written to.
     */
    public void printEvaluationFiles(Set<CrossValidationResult> cvResults, String outputFolder) {
        

        // if (cvResult == null) {
        // LOGGER.warn("cannot print evaluation files because no results were generated yet, run crossValidate before using this");
        // return;
        // }

        StringBuilder csv = new StringBuilder();
        csv.append(evaluationSetting).append("\n");

        // write file1: classifier performances averaged over all datasets, training percentages, and folds
        for (CrossValidationResult cvResult : cvResults) {

            AverageClassifierPerformance avgCP = cvResult.getAveragePerformanceDataSetTrainingFolds();
            csv.append(cvResult.getClassifier()).append(";");
            csv.append(avgCP.getPrecision()).append(";");
            csv.append(avgCP.getRecall()).append(";");
            csv.append(avgCP.getF1()).append("\n");

        }

        FileHelper.writeToFile(outputFolder + File.separator + "averagePerformancesDatasetTrainingFolds.csv", csv);

        csv = new StringBuilder();
        csv.append(evaluationSetting).append("\n");

        // write file2: classifier performances averaged over all training percentages and folds
        for (CrossValidationResult cvResult : cvResults) {

            csv.append(cvResult.getClassifier()).append(";");

            Map<String, AverageClassifierPerformance> avgCP = cvResult.getAveragePerformanceTrainingFolds();
            for (Entry<String, AverageClassifierPerformance> avgEntry : avgCP.entrySet()) {
                csv.append(avgEntry.getKey()).append(";");
                csv.append(avgEntry.getValue().getPrecision()).append(";");
                csv.append(avgEntry.getValue().getRecall()).append(";");
                csv.append(avgEntry.getValue().getF1()).append("\n");
            }

        }
        
        FileHelper.writeToFile(outputFolder + File.separator + "averagePerformancesTrainingFolds.csv", csv);

        csv = new StringBuilder();
        csv.append(evaluationSetting).append("\n");
        
        // write file3: classifier performances averaged over all folds
        for (CrossValidationResult cvResult : cvResults) {

            Map<String, AverageClassifierPerformance> avgCP = cvResult.getAveragePerformanceFolds();
            for (Entry<String, AverageClassifierPerformance> avgEntry : avgCP.entrySet()) {
                csv.append(avgEntry.getKey()).append(";");
                csv.append(avgEntry.getValue().getPrecision()).append(";");
                csv.append(avgEntry.getValue().getRecall()).append(";");
                csv.append(avgEntry.getValue().getF1()).append("\n");
            }

        }
        
        FileHelper.writeToFile(outputFolder + File.separator + "averagePerformancesFolds.csv", csv);

    }


}
