package tud.iir.classification.page.evaluation;

import org.apache.log4j.Logger;

import tud.iir.classification.page.ClassifierManager;
import tud.iir.classification.page.TextClassifier;
import tud.iir.helper.DateHelper;
import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;

public class CrossValidator {

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(CrossValidator.class);

    private EvaluationSetting evaluationSetting;

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
    public void crossValidate(TextClassifier classifier) {

        int kFolds = getEvaluationSetting().getkFolds();
        if (!getEvaluationSetting().isRandom()) {
            kFolds = 1;
        }

        ClassifierManager classifierManager = new ClassifierManager();

        int numberTrainingPercentageLoops = getTrainingPercentageLoops();
        double trainingPercentageMin = getEvaluationSetting().getTrainingPercentageMin();
        double trainingPercentageMax = getEvaluationSetting().getTrainingPercentageMax();
        double trainingPercentageStep = getEvaluationSetting().getTrainingPercentageStep();

        // results[trainingPercentage][threshold][iteration]
        // TODO change to numberLoopsToAverage to add average value in last line
        double[][] openAnalytixPerformances = new double[numberTrainingPercentageLoops][kFolds];
        int[] trainingPercentageUsed = new int[numberTrainingPercentageLoops];

        // iterate over all datasets
        for (Dataset dataset : getEvaluationSetting().getDatasets()) {

            int trainingPercentageLoop = 0;

            // e.g. test from 40:60 to 90:10
            for (double trainingPercentage = trainingPercentageMin; trainingPercentage <= trainingPercentageMax; trainingPercentage += trainingPercentageStep) {

                trainingPercentageUsed[trainingPercentageLoop] = (int) trainingPercentage;
                LOGGER.info("\n start trainingPercentage classification loop on " + dataset.getPath()
                        + " with trainingPercentage "
                                + trainingPercentage + "%, random = " + getEvaluationSetting().isRandom() + "\n");

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



                    StringBuilder trainingsetPercentSB = new StringBuilder();
                    trainingsetPercentSB.append(currentTime);
                    trainingsetPercentSB.append("random trainingPercentage: ").append(trainingPercentage).append("\n");
                    FileHelper.appendToFile("data/temp/thresholds.txt", trainingsetPercentSB, false);

                }

                trainingPercentageLoop++;
            }
        }

        // //print results
        String resultFilePath = "data/temp/" + DateHelper.getCurrentDatetime() + "_results.csv";
        System.out.println("Writing final results to " + resultFilePath);

        String useRandom = getEvaluationSetting().isRandom() ? "random" : "static";

        StringBuilder finalResultSB = new StringBuilder();
        finalResultSB.append("ave perf @ ").append(kFolds).append(";");


        finalResultSB.append("\n");

        for (int i = 0; i < numberTrainingPercentageLoops; i++) {
            finalResultSB.append("train ").append(trainingPercentageUsed[i]).append("% ").append(useRandom).append(";");

                double culmulatedPrecision = 0;
                for (int k = 0; k < kFolds; k++) {
                culmulatedPrecision += openAnalytixPerformances[i][k];
            }
                finalResultSB.append(MathHelper.round(culmulatedPrecision / kFolds, 4)).append(";");

            finalResultSB.append("\n");
            System.out.print("\n");
        }

        FileHelper.writeToFile(resultFilePath, finalResultSB);
    }

    private int getTrainingPercentageLoops() {
        return (int) ((getEvaluationSetting().getTrainingPercentageMax() - getEvaluationSetting()
                .getTrainingPercentageMax())
                / getEvaluationSetting().getTrainingPercentageStep() + 1);
    }

    public void setEvaluationSetting(EvaluationSetting evaluationSetting) {
        this.evaluationSetting = evaluationSetting;
    }

    public EvaluationSetting getEvaluationSetting() {
        return evaluationSetting;
    }

}
