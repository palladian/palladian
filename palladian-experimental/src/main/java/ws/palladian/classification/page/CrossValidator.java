package ws.palladian.classification.page;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.classification.text.evaluation.AverageClassifierPerformance;
import ws.palladian.classification.text.evaluation.ClassifierPerformance;
import ws.palladian.classification.text.evaluation.Dataset;
import ws.palladian.classification.text.evaluation.EvaluationSetting;
import ws.palladian.classification.text.evaluation.TrainingDataSeparation;
import ws.palladian.helper.io.FileHelper;

/**
 * The CrossValidator valdidates a given classifier with the evaluation settings. It can also print results for manual
 * investigation.
 * 
 * @author David Urbansky
 * @author Sandro Reichert
 * 
 * @todo Implement real cross-validation which chooses for a training data percentage of 10% the first 10% in the first
 *           run, the second 10% in the second run etc. This may require an update of class TrainingDataSeparation.
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
    public CrossValidationResult crossValidate(PalladianTextClassifier classifier) {

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

        Dataset trainingTestingDataset = new Dataset();

        // iterate over all datasets
        for (Dataset dataset : getEvaluationSetting().getDatasets()) {

            trainingTestingDataset.setSeparationString(dataset.getSeparationString());
            trainingTestingDataset.setFirstFieldLink(dataset.isFirstFieldLink());

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

                    try {
                        new TrainingDataSeparation().separateFile(dataset.getPath(), "data/temp/cvDataTraining.csv",
                                "data/temp/cvDataTesting.csv", trainingPercentage, getEvaluationSetting().isRandom());
                    } catch (FileNotFoundException e) {
                        LOGGER.error(dataset.getPath() + e.getMessage());
                    } catch (IOException e) {
                        LOGGER.error(dataset.getPath() + e.getMessage());
                    }


                    // classifierManager.setTrainingDataPercentage(100);
                    trainingTestingDataset.setPath("data/temp/cvDataTraining.csv");
                    trainingTestingDataset.setRootPath(FileHelper.getFilePath(dataset.getPath()));
                    classifierManager.trainClassifier(trainingTestingDataset, classifier);

                    // classifierManager.trainAndTestClassifier("data/temp/cvDataTraining.csv",
                    // WebPageClassifier.URL,classType, ClassifierManager.CLASSIFICATION_TRAIN_TEST_SERIALIZE);

                    // classify
                    // classifierManager.setTrainingDataPercentage(0);
                    trainingTestingDataset.setPath("data/temp/cvDataTesting.csv");
                    trainingTestingDataset.setRootPath(FileHelper.getFilePath(dataset.getPath()));
                    classifierManager.testClassifier(trainingTestingDataset, classifier);

                    // classifierManager.trainAndTestClassifier("data/temp/cvDataTesting.csv", WebPageClassifier.URL,
                    // classType, ClassifierManager.CLASSIFICATION_TEST_MODEL);


                    // add the performance to the evaluation maps
                    ClassifierPerformance cfpc = classifier.getPerformanceCopy();

                    performancesDatasetTrainingFolds.add(cfpc);
                    performancesDatasetTrainingFoldsTemp1.add(cfpc);
                }

                // add the performances to the evaluation maps
                HashSet<ClassifierPerformance> cfp = performancesFolds
                        .get(dataset.getPath() + "_"
                                + trainingPercentage);
                if (cfp == null) {
                    cfp = new HashSet<ClassifierPerformance>();
                }
                cfp.addAll(performancesDatasetTrainingFoldsTemp1);
                performancesFolds.put(dataset.getPath() + "_" + trainingPercentage, cfp);

                performancesDatasetTrainingFoldsTemp0.addAll(performancesDatasetTrainingFoldsTemp1);

                trainingPercentageLoop++;
            }

            HashSet<ClassifierPerformance> cfp = performancesTrainingFolds.get(dataset.getPath());
            if (cfp == null) {
                cfp = new HashSet<ClassifierPerformance>();
            }
            cfp.addAll(performancesDatasetTrainingFoldsTemp0);
            performancesTrainingFolds.put(dataset.getPath(), cfp);
        }

        cvResult.setPerformancesDatasetTrainingFolds(performancesDatasetTrainingFolds);
        cvResult.setPerformancesTrainingFolds(performancesTrainingFolds);
        cvResult.setPerformancesFolds(performancesFolds);

        return cvResult;
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

        StringBuilder csv = new StringBuilder();
        csv.append(evaluationSetting).append("\n");

        // write file1: classifier performances averaged over all datasets, training percentages, and folds
        for (CrossValidationResult cvResult : cvResults) {

            AverageClassifierPerformance avgCP = cvResult.getAveragePerformanceDataSetTrainingFolds();
            csv.append(cvResult.getClassifier()).append(";");
            csv.append(cvResult.getFeatureSetting()).append(";");
            csv.append(cvResult.getClassificationTypeSetting()).append(";");
            csv.append(avgCP.getPrecision()).append(";");
            csv.append(avgCP.getRecall()).append(";");
            csv.append(avgCP.getF1()).append("\n");

        }

        FileHelper.writeToFile(outputFolder + File.separator + "averagePerformancesDatasetTrainingFolds.csv", csv);

        csv = new StringBuilder();
        csv.append(evaluationSetting).append("\n");

        // write file2: classifier performances averaged over all training percentages and folds
        for (CrossValidationResult cvResult : cvResults) {

            Map<String, AverageClassifierPerformance> avgCP = cvResult.getAveragePerformanceTrainingFolds();
            for (Entry<String, AverageClassifierPerformance> avgEntry : avgCP.entrySet()) {
                csv.append(avgEntry.getKey()).append(";");
                csv.append(cvResult.getClassifier()).append(";");
                csv.append(cvResult.getFeatureSetting()).append(";");
                csv.append(cvResult.getClassificationTypeSetting()).append(";");
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
                csv.append(cvResult.getClassifier()).append(";");
                csv.append(cvResult.getFeatureSetting()).append(";");
                csv.append(cvResult.getClassificationTypeSetting()).append(";");
                csv.append(avgEntry.getValue().getPrecision()).append(";");
                csv.append(avgEntry.getValue().getRecall()).append(";");
                csv.append(avgEntry.getValue().getF1()).append("\n");
            }

        }

        FileHelper.writeToFile(outputFolder + File.separator + "averagePerformancesFolds.csv", csv);

    }


}
