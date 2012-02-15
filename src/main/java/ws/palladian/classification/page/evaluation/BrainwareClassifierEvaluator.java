package ws.palladian.classification.page.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import ws.palladian.classification.ClassifierPerformanceResult;
import ws.palladian.classification.Instances;
import ws.palladian.classification.UniversalInstance;
import ws.palladian.classification.page.DictionaryClassifier;
import ws.palladian.classification.page.TextClassifier;
import ws.palladian.classification.page.TextInstance;
import ws.palladian.helper.DatasetManager;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.math.Matrix;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.preprocessing.ProcessingPipeline;
import ws.palladian.preprocessing.StopWordRemover;

public class BrainwareClassifierEvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(BrainwareClassifierEvaluator.class);

    private int crossValidation = 5;

    /** The list of classifiers to evaluate. */
    private final List<TextClassifier> classifiers = new ArrayList<TextClassifier>();

    /** The list of dataset to use for evaluation. */
    private final List<Dataset> datasets = new ArrayList<Dataset>();

    /**
     * The number of instances per class in the dataset to use for evaluation. This number must be lower or equals the
     * number of instances in the smallest dataset. -1 means that all instances should be considered.
     */
    private int numberOfInstancesPerClass = -1;

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

            // we need to copy the classifier since we reset it for evaluation which makes the original classifier
            // useless
            TextClassifier evalClassifier = classifier.copy();

            for (Dataset dataset : datasets) {

                // collect the classifier performance for each fold, merge them in the end
                List<ClassifierPerformanceResult> performances = new ArrayList<ClassifierPerformanceResult>();

                // get the files for cross validation
                List<String[]> fileSplits = new ArrayList<String[]>();
                try {
                    fileSplits = dsManager.splitForCrossValidation(dataset, getCrossValidation(),
                            getNumberOfInstancesPerClass());
                } catch (IOException e) {
                    LOGGER.error("could not split dataset for cross validation, " + e.getMessage());
                }

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

                    evalClassifier.train(trainingDataset);

                    ClassifierPerformanceResult performance = evalClassifier.evaluate(testDataset)
                            .getClassifierPerformanceResult();
                    
                    System.out.println(performance.getConfusionMatrix());

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
            results.append(dataset).append(";;;;;;;;");
        }
        results.append("\n");
        for (int i = 0; i < datasets.size(); i++) {
            results.append(";Precision;Recall;F1;Sensitivity;Specificity;Acurracy;Correctness;Superiority");
        }
        results.append("\n");

        for (TextClassifier classifier : classifiers) {
            results.append(classifier.toString());
            for (Dataset dataset : datasets) {
                results.append(";").append(evaluationMatrix.get(dataset.getName(), classifier.getName()));
            }
            results.append("\n");
        }
        
        // add all confusion matrices for all classifier-dataset combinations
        results.append("\n");
        for (TextClassifier classifier : classifiers) {
            for (Dataset dataset : datasets) {
                results.append(classifier.toString() + " - " + dataset.getName() + " confusion matrix\n");
                ClassifierPerformanceResult classificationResult = (ClassifierPerformanceResult) evaluationMatrix.get(dataset.getName(), classifier.getName());
                results.append(classificationResult.getConfusionMatrix().asCsv());
                results.append("\n\n");
            }
            results.append("\n");
        }

        // add data for threshold analysis for all classifier-dataset combinations
        for (TextClassifier classifier : classifiers) {
            for (Dataset dataset : datasets) {
                ClassifierPerformanceResult classificationResult = (ClassifierPerformanceResult)evaluationMatrix.get(
                        dataset.getName(), classifier.getName());

                results.append(classifier.toString() + " - " + dataset.getName() + " threshold bucket analysis\n");
                results.append(classificationResult.getThresholdBucketMapAsCsv());
                results.append("\n\n");

                results.append(classifier.toString() + " - " + dataset.getName() + " threshold accumulative analysis\n");
                results.append(classificationResult.getThresholdAccumulativeMapAsCsv());
                results.append("\n\n");
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
        
        double superiority = 0.0;
        
        ConfusionMatrix confusionMatrix = null;

        Map<Double, Double[]> thresholdBucketMap = null;
        Map<Double, Double[]> thresholdAccMap = null;

        for (ClassifierPerformanceResult classifierPerformanceResult : performances) {
            precision += classifierPerformanceResult.getPrecision();
            recall += classifierPerformanceResult.getRecall();
            f1 += classifierPerformanceResult.getF1();
            sensitivity += classifierPerformanceResult.getSensitivity();
            specificity += classifierPerformanceResult.getSpecificity();
            accuracy += classifierPerformanceResult.getAccuracy();
            correctness += classifierPerformanceResult.getCorrectlyClassified();
            superiority += classifierPerformanceResult.getSuperiority();
            
            // merge confusion matrix
            if (confusionMatrix == null) {
                confusionMatrix = classifierPerformanceResult.getConfusionMatrix();
            } else {
                confusionMatrix.add(classifierPerformanceResult.getConfusionMatrix());
            }
            
            // merge threshold bucket map
            if (thresholdBucketMap == null) {
                thresholdBucketMap = classifierPerformanceResult.getThresholdBucketMap();
            } else {

                // add all entries
                Iterator<Double[]> iterator1 = thresholdBucketMap.values().iterator();
                Iterator<Double[]> iterator2 = classifierPerformanceResult.getThresholdBucketMap().values().iterator();
                while (iterator1.hasNext() && iterator2.hasNext()) {
                    Double[] valuesCurrent = iterator1.next();
                    Double[] valuesToAdd = iterator2.next();

                    for (int i = 0; i < valuesCurrent.length; i++) {
                        valuesCurrent[i] += valuesToAdd[i];
                    }
                }
            }

            // merge threshold accumulative map
            if (thresholdAccMap == null) {
                thresholdAccMap = classifierPerformanceResult.getThresholdAccumulativeMap();
            } else {
                // add all entries
                Iterator<Double[]> iterator1 = thresholdAccMap.values().iterator();
                Iterator<Double[]> iterator2 = classifierPerformanceResult.getThresholdAccumulativeMap().values()
                        .iterator();
                while (iterator1.hasNext() && iterator2.hasNext()) {
                    Double[] valuesCurrent = iterator1.next();
                    Double[] valuesToAdd = iterator2.next();

                    for (int i = 0; i < valuesCurrent.length; i++) {
                        valuesCurrent[i] += valuesToAdd[i];
                    }
                }
            }

        }

        // divide all summed up values by the number of performances
        precision /= performances.size();
        recall /= performances.size();
        f1 /= performances.size();
        sensitivity /= performances.size();
        specificity /= performances.size();
        accuracy /= performances.size();
        correctness /= performances.size();
        superiority /= performances.size();
        confusionMatrix.divideBy(performances.size());
        
        result.setPrecision(precision);
        result.setRecall(recall);
        result.setF1(f1);
        result.setSensitivity(sensitivity);
        result.setSpecificity(specificity);
        result.setAccuracy(accuracy);
        result.setCorrectlyClassified(correctness);
        result.setSuperiority(superiority);
        result.setConfusionMatrix(confusionMatrix);
        
        for (Entry<Double, Double[]> entry : thresholdBucketMap.entrySet()) {

            Double[] values = entry.getValue();

            for (int i = 0; i < values.length; i++) {
                values[i] /= (double)performances.size();
            }
        }
        result.setThresholdBucketMap(thresholdBucketMap);

        for (Entry<Double, Double[]> entry : thresholdAccMap.entrySet()) {

            Double[] values = entry.getValue();

            for (int i = 0; i < values.length; i++) {
                values[i] /= (double)performances.size();
            }
        }
        result.setThresholdAccumulativeMap(thresholdAccMap);

        return result;
    }

    public int getCrossValidation() {
        return crossValidation;
    }

    public void setCrossValidation(int crossValidation) {
        this.crossValidation = crossValidation;
    }

    public void setNumberOfInstancesPerClass(int numberOfInstancesPerClass) {
        this.numberOfInstancesPerClass = numberOfInstancesPerClass;
    }

    public int getNumberOfInstancesPerClass() {
        return numberOfInstancesPerClass;
    }

    /**
     * Evaluate the classifier playground. Do not use...just for quality control and manual testing.
     * 
     * @throws IOException
     * 
     */
    public void evaluateClassifierPlayground() throws IOException {

        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new StopWordRemover());

        // pipeline.add(new WordCounter());

        DictionaryClassifier textClassifier = new DictionaryClassifier();
        textClassifier.setName("D1");
        textClassifier.getFeatureSetting().setMinNGramLength(1);
        textClassifier.getFeatureSetting().setMaxNGramLength(1);
        textClassifier.getFeatureSetting().setTextFeatureType(FeatureSetting.WORD_NGRAMS);
        textClassifier.setProcessingPipeline(pipeline);
        textClassifier.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);

        // get all training and test files
        List<String> trainingFileList = FileHelper
                .readFileToArray("data/datasets/classification/brainware/ListOfLearnedBooks.txt");
        Collection<File> trainingFiles = new HashSet<File>();
        Collection<File> testFiles = new HashSet<File>();
        
        File[] files = FileHelper.getFiles("data/datasets/classification/brainware/books/", "", true);
        ol: for (File file : files) {
            if (file.isDirectory()) {
                continue;
            }
            for (String trainingFileName : trainingFileList) {
                if (file.getAbsolutePath().contains(trainingFileName.replace("/", "\\"))) {
                    trainingFiles.add(file);
                    // break ol;
                } else {
                    testFiles.add(file);
                }
            }
        }
        
        Instances<UniversalInstance> textInstances = new Instances<UniversalInstance>();
        for (File file : trainingFiles) {
            String text = FileHelper.readFileToString(file);

            // String[] parts = file.getPath().split(File.pathSeparator);
            String categoryName = StringHelper.getSubstringBetween(file.getPath(), "books\\", "\\");

            UniversalInstance textInstance = new UniversalInstance(textInstances);
            textInstance.setTextFeature(text);
            textInstance.setInstanceCategory(categoryName);
            textInstances.add(textInstance);
        }
        
        textClassifier.addTrainingInstances(textInstances);
        textClassifier.train(true);
        FileHelper.serialize(textClassifier, "data/textClassifier.gz");

        // read test instances
        Instances<UniversalInstance> testInstances = new Instances<UniversalInstance>();
        for (File file : testFiles) {
            String text = FileHelper.readFileToString(file);

            // String[] parts = file.getPath().split(File.pathSeparator);
            String categoryName = StringHelper.getSubstringBetween(file.getPath(), "books\\", "\\");

            UniversalInstance textInstance = new UniversalInstance(testInstances);
            textInstance.setTextFeature(text);
            textInstance.setInstanceCategory(categoryName);
            testInstances.add(textInstance);
        }
        System.out.println(textClassifier.evaluate(testInstances));

        // FileHelper.serialize(textClassifier,"dc2.gz");
        // ClassifierPerformance evaluate = textClassifier.evaluate(dataset);
        // System.out.println(evaluate);
        // System.exit(0);
    }

    /**
     * @param args
     * @throws IOException
     */
    public static void main(String[] args) throws IOException {
        BrainwareClassifierEvaluator evaluator = new BrainwareClassifierEvaluator();

        evaluator.evaluateClassifierPlayground();
        System.exit(0);
        
        StringBuilder results = new StringBuilder();
        DictionaryClassifier dc2 = FileHelper.deserialize("dc2.gz");
        List<String> articles = FileHelper.readFileToArray("C:\\My Dropbox\\brainware\\articles.csv");
        int total = articles.size();
        int c = 0;
        for (String line : articles) {
            String[] parts = line.split("<###>");
            TextInstance result = dc2.classify(parts[0]);
            result.getMainCategoryEntry().getRelevance();
            results.append(parts[1]).append("\t").append(result.getMainCategoryEntry().getCategory().getName()).append("\n");
            c++;
            if (c % 1000 == 0) {
                System.out.println(MathHelper.round(100*c/(double) total, 2));
            }
        }
        FileHelper.writeToFile("results.csv", results);
        System.exit(0);

        evaluator.setCrossValidation(2);
        evaluator.addClassifier(new DictionaryClassifier());
        Dataset dataset = new Dataset();
        dataset.setPath("data/temp/training.csv");
        evaluator.addDataset(dataset);
        evaluator.runEvaluation("data/temp/evaluatorResults.csv");
    }

}
