package ws.palladian.classification.text.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import ws.palladian.classification.ClassifierPerformanceResult;
import ws.palladian.classification.DatasetManager;
import ws.palladian.classification.text.PalladianTextClassifier;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.math.ConfusionMatrix;
import ws.palladian.helper.math.Matrix;

public class ClassifierEvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(ClassifierEvaluator.class);

    private int crossValidation = 5;

    /** The list of classifiers to evaluate. */
    private final List<PalladianTextClassifier> classifiers = new ArrayList<PalladianTextClassifier>();

    /** The list of dataset to use for evaluation. */
    private final List<Dataset> datasets = new ArrayList<Dataset>();

    /**
     * The number of instances per class in the dataset to use for evaluation. This number must be lower or equals the
     * number of instances in the smallest dataset. -1 means that all instances should be considered.
     */
    private int numberOfInstancesPerClass = -1;

    public void addClassifier(PalladianTextClassifier classifier) {
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
        for (PalladianTextClassifier classifier : classifiers) {

            // we need to copy the classifier since we reset it for evaluation which makes the original classifier
            // useless
            PalladianTextClassifier evalClassifier = classifier.copy();

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

                    // FIXME next couple of lines!
                    // ClassifierPerformanceResult performance =
                    // evalClassifier.evaluate(testDataset).getClassifierPerformanceResult();
                    //
                    // System.out.println(performance.getConfusionMatrix());
                    //
                    // performances.add(performance);
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

        for (PalladianTextClassifier classifier : classifiers) {
            results.append(classifier.toString());
            for (Dataset dataset : datasets) {
                results.append(";").append(evaluationMatrix.get(dataset.getName(), classifier.getName()));
            }
            results.append("\n");
        }

        // add all confusion matrices for all classifier-dataset combinations
        results.append("\n");
        for (PalladianTextClassifier classifier : classifiers) {
            for (Dataset dataset : datasets) {
                results.append(classifier.toString() + " - " + dataset.getName() + " confusion matrix\n");
                ClassifierPerformanceResult classificationResult = (ClassifierPerformanceResult)evaluationMatrix.get(
                        dataset.getName(), classifier.getName());
                results.append(classificationResult.getConfusionMatrix().asCsv());
                results.append("\n\n");
            }
            results.append("\n");
        }

        // add data for threshold analysis for all classifier-dataset combinations
        for (PalladianTextClassifier classifier : classifiers) {
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

    // public void createModel() {
    // String classifierName = "germanSentimentWeblogs.gz";
    //
    // // classifier
    // DictionaryClassifier classifier = new DictionaryClassifier();
    // classifier.setName("D1");
    // classifier.getFeatureSetting().setTextFeatureType(FeatureSetting.CHAR_NGRAMS);
    // // classifier.getFeatureSetting().setMaxTerms(10);
    // classifier.getFeatureSetting().setMinNGramLength(3);
    // classifier.getFeatureSetting().setMaxNGramLength(7);
    // classifier.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);
    //
    // // dataset
    // Dataset dataset = new Dataset("Dataset");
    // dataset.setFirstFieldLink(false);
    // dataset.setSeparationString("<###>");
    // dataset.setPath("H:\\PalladianData\\Datasets\\SentimentDatasets\\German\\WebBlogsNews.csv");
    //
    // // train
    // classifier.train(dataset);
    // FileHelper.serialize(classifier, classifierName);
    //
    // // evaluate
    // ClassifierPerformance result = classifier.evaluate(dataset);
    // System.out.println(result);
    // }
    //
    // /**
    // * Evaluate the classifier playground. Do not use...just for quality control and manual testing.
    // *
    // * @throws IOException
    // *
    // */
    // public void evaluateClassifierPlayground() throws IOException {
    //
    // ProcessingPipeline pipeline = new ProcessingPipeline();
    // // pipeline.add(new StopWordRemover());
    //
    // // pipeline.add(new WordCounter());
    //
    // DictionaryClassifier dictionaryClassifier1 = new DictionaryClassifier();
    // dictionaryClassifier1.setName("D1");
    // dictionaryClassifier1.getFeatureSetting().setTextFeatureType(FeatureSetting.WORD_NGRAMS);
    // dictionaryClassifier1.getFeatureSetting().setMaxTerms(10);
    // dictionaryClassifier1.getFeatureSetting().setMinNGramLength(1);
    // dictionaryClassifier1.getFeatureSetting().setMaxNGramLength(1);
    // dictionaryClassifier1.setProcessingPipeline(pipeline);
    // dictionaryClassifier1.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);
    //
    // DictionaryClassifier dictionaryClassifier2 = new DictionaryClassifier();
    // dictionaryClassifier2.setName("D2");
    // dictionaryClassifier2.getFeatureSetting().setTextFeatureType(FeatureSetting.WORD_NGRAMS);
    // dictionaryClassifier2.getFeatureSetting().setMinNGramLength(1);
    // dictionaryClassifier2.getFeatureSetting().setMaxNGramLength(3);
    // dictionaryClassifier2.setProcessingPipeline(pipeline);
    // // dictionaryClassifier2.getClassificationTypeSetting().setClassificationType(ClassificationTypeSetting.TAG);
    //
    // DictionaryClassifier dictionaryClassifier3 = new DictionaryClassifier();
    // dictionaryClassifier3.setName("D3");
    // // dictionaryClassifier3.getFeatureSetting().setTextFeatureType(FeatureSetting.WORD_NGRAMS);
    // dictionaryClassifier3.getFeatureSetting().setMinNGramLength(2);
    // dictionaryClassifier3.getFeatureSetting().setMaxNGramLength(6);
    // DictionaryClassifier dictionaryClassifier4 = new DictionaryClassifier();
    // dictionaryClassifier4.setName("D4");
    // dictionaryClassifier4.getFeatureSetting().setMinNGramLength(2);
    // dictionaryClassifier4.getFeatureSetting().setMaxNGramLength(8);
    //
    // Dataset dataset = new Dataset("Preisroboter");
    // dataset.setFirstFieldLink(false);
    // dataset.setSeparationString("<###>");
    // dataset.setPath("data/temp/amazon/amazonElectronicDE_detailedCats.csv");
    // dataset.setPath("data/temp/amazon/amazonElectronicDE_mainCats.csv");
    // dataset.setPath("data/temp/amazon/amazonElectronicDE_selectedCats.csv");
    // dataset.setPath("data/temp/amazon/amazonElectronicDE_selectedCats.csv");
    // // dataset.setPath("data/datasets/classification/SentimentSentences.csv");
    // dataset.setPath("data/temp/products_clean_id.csv");
    // dataset.setPath("C:\\My Dropbox\\brainware\\articles.csv");
    // dataset.setPath("mjtrain.csv");
    //
    // Dataset dataset2 = new Dataset("Schottenland");
    // dataset2.setFirstFieldLink(false);
    // dataset2.setSeparationString("<###>");
    // dataset2.setPath("data/temp/products_clean_id.csv");
    //
    // // dataset.setPath("data/temp/articles_small.csv");
    // // dataset.setPath("data/temp/trainingCollection.csv");
    // // dataset.setPath("data/temp/trainingCollection2.csv");
    // // dataset.setPath("data/temp/dataset_classifier_dev_1_ipc1000.csv");
    //
    // DatasetManager datasetManager = new DatasetManager();
    //
    // // create an excerpt (optional)
    // // String dsExcerpt = datasetManager.createIndexExcerptRandom(dataset2.getPath(),
    // // dataset2.getSeparationString(), 5000);
    // String dsExcerpt = datasetManager.createIndexExcerpt(dataset.getPath(), dataset.getSeparationString(), 500);
    // dataset2.setPath(dsExcerpt);
    // // datasetManager.calculateClassDistribution(dataset, "data/temp/schottenland/distributionExcerpt.csv");
    //
    // // datasetManager.calculateClassDistribution(dataset, "data/temp/schottenland/distributionFull.csv");
    // datasetManager.calculateClassDistribution(dataset2,
    // "data/datasets/classification/schottenland_distribution.csv");
    //
    // int countClasses = datasetManager.countClasses(dataset);
    // System.out.println("The dataset " + dataset.getName() + " contains " + countClasses + " classes");
    // // System.exit(0);
    //
    // ClassifierEvaluator evaluator = new ClassifierEvaluator();
    // evaluator.setCrossValidation(3);
    // // evaluator.setNumberOfInstancesPerClass(50);
    //
    // dictionaryClassifier1.train(dataset);
    //
    // FileHelper.serialize(dictionaryClassifier1, "dc2.gz");
    // dataset2.setPath("mjtest.csv");
    // ClassifierPerformance evaluate = dictionaryClassifier1.evaluate(dataset2);
    // System.out.println(evaluate);
    // System.exit(0);
    //
    // evaluator.addClassifier(dictionaryClassifier1);
    // // evaluator.addClassifier(dictionaryClassifier2);
    // // evaluator.addClassifier(dictionaryClassifier3);
    // // evaluator.addClassifier(dictionaryClassifier4);
    // // evaluator.addDataset(dataset);
    // evaluator.addDataset(dataset2);
    //
    // // evaluator.runEvaluation("data/temp/schottenland/evaluatorResults.csv");
    // evaluator.runEvaluation("data/temp/evaluationResults.csv");
    // System.exit(0);
    //
    // dictionaryClassifier2.train(dataset2);
    //
    // // ClassifierManager classifierManager = new ClassifierManager();
    // // classifierManager.trainClassifier(dataset, dictionaryClassifier1);
    //
    // FileHelper.serialize(dictionaryClassifier2, "senticlassifier.gz");
    // // FileHelper.serialize(dictionaryClassifier2, "slc10k.gz");
    // // FileHelper.serialize(dictionaryClassifier1, "prcSelected10k.gz");
    // // FileHelper.serialize(dictionaryClassifier1, "topicClassifier.gz");
    // // LOGGER.info(evaluationMatrix.get(dataset.getName(), dictionaryClassifier1.getName()));
    // // LOGGER.info(evaluationMatrix.get(dataset.getName(), dictionaryClassifier2.getName()));
    // }
    //
    // public void testNB() {
    //
    // List<NominalInstance> instances = new ArrayList<NominalInstance>();
    //
    // List<String> lines = FileHelper.readFileToArray("mjtrain.csv");
    // for (String line : lines) {
    // NominalInstance instance = new NominalInstance();
    //
    // String[] parts = line.split("<###>");
    // String[] words = parts[0].split("\\s");
    // instance.targetClass = parts[1];
    // instance.featureVector = new FeatureVector();
    //
    // if (words.length > 10) {
    // for (int i = 0; i < 10; i++) {
    // instance.featureVector.add(new NominalFeature(FeatureDescriptorBuilder.build(String.valueOf(i),
    // NominalFeature.class), words[i]));
    // }
    // }
    //
    // instances.add(instance);
    // }
    //
    // NaiveBayesClassifier nbc = new NaiveBayesClassifier();
    // NaiveBayesModel model = nbc.learn(instances);
    //
    // int correct = 0;
    // int t = 0;
    // lines = FileHelper.readFileToArray("mjtest.csv");
    // for (String line : lines) {
    // FeatureVector instance = new FeatureVector();
    //
    // String[] parts = line.split("<###>");
    // String[] words = parts[0].split("\\s");
    //
    // if (words.length > 10) {
    // for (int i = 0; i < 10; i++) {
    // instance.add(new NominalFeature(FeatureDescriptorBuilder.build(String.valueOf(i),
    // NominalFeature.class),words[i]));
    // }
    // t++;
    // }
    //
    // CategoryEntries entries = nbc.predict(instance, model);
    //
    // if (ClassificationUtils.getSingleBestCategoryEntry(entries).getCategory().getName().equalsIgnoreCase(parts[1])) {
    // correct++;
    // }
    // }
    //
    // System.out.println(correct / (double)t);
    //
    // }
    //
    // public void evaluateClassifierPlaygroundRegression() throws IOException {
    //
    // DictionaryClassifier dictionaryClassifier1 = new DictionaryClassifier();
    // dictionaryClassifier1.setName("D1");
    // dictionaryClassifier1.getFeatureSetting().setMinNGramLength(3);
    // dictionaryClassifier1.getFeatureSetting().setMaxNGramLength(5);
    // dictionaryClassifier1.getClassificationTypeSetting()
    // .setClassificationType(ClassificationTypeSetting.REGRESSION);
    //
    // Dataset dataset = new Dataset("Movies");
    // dataset.setFirstFieldLink(false);
    // dataset.setSeparationString("###");
    // dataset.setPath("C:\\Data\\datasets\\MovieRatings\\movieRatingsTitle.csv");
    //
    // DatasetManager datasetManager = new DatasetManager();
    //
    // // create an excerpt (optional)
    // // String dsExcerpt = datasetManager.createIndexExcerptRandom(dataset2.getPath(),
    // // dataset2.getSeparationString(), 5000);
    // String dsExcerpt = datasetManager.createIndexExcerpt(dataset.getPath(), dataset.getSeparationString(), 500);
    // // /dataset2.setPath(dsExcerpt);
    // // datasetManager.calculateClassDistribution(dataset, "data/temp/schottenland/distributionExcerpt.csv");
    //
    // ClassifierEvaluator evaluator = new ClassifierEvaluator();
    // evaluator.setCrossValidation(3);
    //
    // List<String> array = FileHelper.readFileToArray("C:\\Data\\datasets\\MovieRatings\\movieRatingsTitle.csv");
    //
    // Instances<UniversalInstance> trainInstances = new Instances<UniversalInstance>();
    // for (String line : array) {
    //
    // String[] parts = line.split("###");
    //
    // UniversalInstance trainInstance = new UniversalInstance(trainInstances);
    //
    // String text = parts[0];
    // String category = parts[1];
    //
    // trainInstance.setTextFeature(text);
    // trainInstance.setInstanceCategory(category);
    // trainInstances.add(trainInstance);
    // }
    //
    // // train the classifier
    // dictionaryClassifier1.setTrainingInstances(trainInstances);
    // dictionaryClassifier1.train();
    //
    // // dictionaryClassifier1.train(dataset);
    // // FileHelper.serialize(dictionaryClassifier1,"dc2Reg.gz");
    // //
    // // dictionaryClassifier1 = FileHelper.deserialize("dc2Reg.gz");
    // //
    // TextInstance classify = dictionaryClassifier1.classify("The last song");
    // //CollectionHelper.print(classify.getMainCategoryEntry().getCategory().getName());
    // System.out.println(classify.getMainCategoryEntry().getCategory().getName());
    //
    // System.exit(0);
    //
    // }

    /**
     * @param args
     * @throws IOException
     */
    // public static void main(String[] args) throws IOException {
    // ClassifierEvaluator evaluator = new ClassifierEvaluator();
    //
    // // evaluator.evaluateClassifierPlaygroundRegression();
    // // evaluator.evaluateClassifierPlayground();
    // evaluator.createModel();
    // // evaluator.testNB();
    // System.exit(0);
    //
    // StringBuilder results = new StringBuilder();
    // DictionaryClassifier dc2 = FileHelper.deserialize("dc2.gz");
    // List<String> articles = FileHelper.readFileToArray("C:\\My Dropbox\\brainware\\articles.csv");
    // int total = articles.size();
    // int c = 0;
    // for (String line : articles) {
    // String[] parts = line.split("<###>");
    // TextInstance result = dc2.classify(parts[0]);
    // result.getMainCategoryEntry().getRelevance();
    // results.append(parts[1]).append("\t").append(result.getMainCategoryEntry().getCategory().getName())
    // .append("\n");
    // c++;
    // if (c % 1000 == 0) {
    // System.out.println(MathHelper.round(100 * c / (double) total, 2));
    // }
    // }
    // FileHelper.writeToFile("results.csv", results);
    // System.exit(0);
    //
    // evaluator.setCrossValidation(2);
    // evaluator.addClassifier(new DictionaryClassifier());
    // Dataset dataset = new Dataset();
    // dataset.setPath("data/temp/training.csv");
    // evaluator.addDataset(dataset);
    // evaluator.runEvaluation("data/temp/evaluatorResults.csv");
    // }

}
