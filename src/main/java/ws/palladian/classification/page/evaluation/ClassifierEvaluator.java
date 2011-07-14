package ws.palladian.classification.page.evaluation;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.classification.ClassifierPerformanceResult;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextClassifier;
import ws.palladian.helper.DatasetManager;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.math.Matrix;

public class ClassifierEvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ClassifierEvaluator.class);

    private int crossValidation = 5;

    /** The list of classifiers to evaluate. */
    private List<TextClassifier> classifiers = new ArrayList<TextClassifier>();

    /** The list of dataset to use for evaluation. */
    private List<Dataset> datasets = new ArrayList<Dataset>();
    
    /** The number of instances per dataset to use for evaluation. This number must be lower or equals the number of instances in the smallest dataset. -1 means that all instances should be considered. */
    private int numberOfInstances = -1;

    public void addClassifier(TextClassifier classifier) {
        classifiers.add(classifier);
    }

    public void addDataset(Dataset dataset) {
        datasets.add(dataset);
    }

    public Matrix runEvaluation(String evaluationOutputPath) {
        StopWatch stopWatch = new StopWatch();

        Matrix evaluationMatrix = new Matrix();

        DatasetManager dsManager = new DatasetManager();

        // loop through all classifiers
        for (TextClassifier classifier : classifiers) {

            // we need to copy the classifier since we reset it for evaluation which makes the original classifier useless
            TextClassifier evalClassifier = classifier.copy();
            
            for (Dataset dataset : datasets) {

                // collect the classifier performance for each fold, merge them in the end
                List<ClassifierPerformanceResult> performances = new ArrayList<ClassifierPerformanceResult>();

                // get the files for cross validation
                List<String[]> fileSplits = dsManager.splitForCrossValidation(dataset, getCrossValidation());

                // iterate through the cross validation folds, each entry contains the training and test file path
                for (String[] filePaths : fileSplits) {

                    // we need to reset the classifier to avoid keeping training data from the last round
                    evalClassifier.reset();

                    // System.out.println(classifier.getTrainingDocuments().size());
                    // System.out.println(classifier.getTrainingInstances().size());
                    // System.out.println(classifier.getTestDocuments().size());
                    // System.out.println(classifier.getTestInstances().size());
                    // System.out.println(((DictionaryClassifier) classifier).getDictionary().size());
                    // System.out.println(classifier.getPerformance());

                    // create the training dataset from the current split
                    Dataset trainingDataset = new Dataset();
                    trainingDataset.setFirstFieldLink(dataset.isFirstFieldLink());
                    trainingDataset.setSeparationString(dataset.getSeparationString());
                    trainingDataset.setPath(filePaths[0]);

                    // create the test dataset from the current split
                    Dataset testDataset = new Dataset();
                    testDataset.setFirstFieldLink(dataset.isFirstFieldLink());
                    testDataset.setSeparationString(dataset.getSeparationString());
                    testDataset.setPath(filePaths[1]);
                    
                    evalClassifier.train(trainingDataset, getNumberOfInstances());

                    ClassifierPerformanceResult performance = evalClassifier.evaluate(testDataset)
                    .getClassifierPerformanceResult();

                    // System.out.println(performance);

                    performances.add(performance);
                }

                ClassifierPerformanceResult averagedPerformance = averageClassifierPerformances(performances);

                evaluationMatrix.set(dataset.getName(), evalClassifier.getName(), averagedPerformance);
            }

            // free memory by resetting the classifier (training and test documents will be deleted)
            evalClassifier.reset();
        }

        // write results
        StringBuilder results = new StringBuilder();

        results.append("Evaluation of " + classifiers.size() + " classifiers on " + datasets.size() + " datasets.\n");
        results.append("Cross validation folds per dataset: " + getCrossValidation() + "\n");
        results.append("Time taken: " + stopWatch.getTotalElapsedTimeString() + "\n\n");

        results.append(";");
        for (Dataset dataset : datasets) {
            results.append(dataset).append(";;;;;;;");
        }
        results.append("\n;");
        for (int i = 0; i < datasets.size(); i++) {
            results.append("Precision;Recall;F1;Sensitivity;Specificity;Acurracy;Correctness");
        }
        results.append("\n");

        for (TextClassifier classifier : classifiers) {
            results.append(classifier.toString()).append(";");
            for (Dataset dataset : datasets) {
                results.append(evaluationMatrix.get(dataset.getName(), classifier.toString()));
            }
            results.append("\n");
        }

        results.append("\nAll scores are averaged over all classes in the dataset and weighted by their priors.");
        FileHelper.writeToFile(evaluationOutputPath, results);

        LOGGER.info("complete evaluation on " + classifiers.size() + " classifiers and " + datasets.size()
                + " datasets with " + getCrossValidation() + " cv folds took " + stopWatch.getTotalElapsedTimeString());

        return evaluationMatrix;
    }

    private ClassifierPerformanceResult averageClassifierPerformances(List<ClassifierPerformanceResult> performances) {
        ClassifierPerformanceResult result = new ClassifierPerformanceResult();

        double precision = 0.0;
        double recall = 0.0;
        double f1 = 0.0;

        double sensitivity = 0.0;
        double specificity = 0.0;
        double accuracy = 0.0;

        double correctness = 0.0;

        for (ClassifierPerformanceResult classifierPerformanceResult : performances) {
            precision += classifierPerformanceResult.getPrecision();
            recall += classifierPerformanceResult.getRecall();
            f1 += classifierPerformanceResult.getF1();
            sensitivity += classifierPerformanceResult.getSensitivity();
            specificity += classifierPerformanceResult.getSpecificity();
            accuracy += classifierPerformanceResult.getAccuracy();
            correctness += classifierPerformanceResult.getCorrectlyClassified();
        }

        precision /= performances.size();
        recall /= performances.size();
        f1 /= performances.size();
        sensitivity /= performances.size();
        specificity /= performances.size();
        accuracy /= performances.size();
        correctness /= performances.size();

        result.setPrecision(precision);
        result.setRecall(recall);
        result.setF1(f1);
        result.setSensitivity(sensitivity);
        result.setSpecificity(specificity);
        result.setAccuracy(accuracy);
        result.setCorrectlyClassified(correctness);

        return result;
    }

    public int getCrossValidation() {
        return crossValidation;
    }

    public void setCrossValidation(int crossValidation) {
        this.crossValidation = crossValidation;
    }

    public void setNumberOfInstances(int numberOfInstances) {
        this.numberOfInstances = numberOfInstances;
    }

    public int getNumberOfInstances() {
        return numberOfInstances;
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        ClassifierEvaluator evaluator = new ClassifierEvaluator();
        evaluator.setCrossValidation(2);
        evaluator.addClassifier(new DictionaryClassifier());
        Dataset dataset = new Dataset();
        dataset.setPath("data/temp/training.csv");
        evaluator.addDataset(dataset);
        evaluator.runEvaluation("data/temp/evaluatorResults.csv");
    }

}
