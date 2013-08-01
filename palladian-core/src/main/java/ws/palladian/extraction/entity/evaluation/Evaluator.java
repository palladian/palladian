
package ws.palladian.extraction.entity.evaluation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.ContextAnnotation;
import ws.palladian.extraction.entity.FileFormatParser;
import ws.palladian.extraction.entity.NamedEntityRecognizer;
import ws.palladian.extraction.entity.TaggingFormat;
import ws.palladian.extraction.entity.TrainableNamedEntityRecognizer;
import ws.palladian.extraction.entity.dataset.DatasetProcessor;
import ws.palladian.extraction.entity.evaluation.EvaluationResult.EvaluationMode;
import ws.palladian.extraction.entity.tagger.OpenNlpNer;
import ws.palladian.extraction.entity.tagger.PalladianNer;
import ws.palladian.extraction.entity.tagger.PalladianNer.LanguageMode;
import ws.palladian.extraction.entity.tagger.PalladianNer.TrainingMode;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.features.Annotation;

/**
 * @author David Urbansky
 * 
 */
public class Evaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Evaluator.class);

    private static final String EVALUATION_PATH = "data/temp/nerEvaluation/";

    /**
     * <p>
     * Seed-only evaluation. In this evaluation scenario, the NER is only trained on a set of seed entities per concept.
     * Most NERs can not be trained in this fashion so we only evaluate {@link PalladianNer}.
     * </p>
     * 
     * <p>
     * We evaluate on the complete test set and once more only on entities which were not used for the training (direct
     * name comparison, e.g. when we used "Jim Carrey (PER)" as a training seed we do not evaluate on "Jim Carrey (X)"
     * for all X.).
     * </p>
     * 
     * <p>
     * This evaluation method will generate two csv files in the following format:
     * 
     * <pre>
     *                 |                     Complete Test Set                                     |                           Unseen entities only
     * Number of Seeds | Ex. Precision | Ex. Recall | Ex. F1 | MUC Precision | MUC Recall | MUC F1 | Ex. Precision | Ex. Recall | Ex. F1 | MUC Precision | MUC Recall | MUC F1
     * minNumberOfSeeds|
     * ...             |
     * maxNumberOfSeeds|
     * </pre>
     * 
     * One file is for the TUDNER in English mode and the other one for language independent mode. The result file will
     * be written to EVALUATION_PATH.
     * </p>
     * 
     * @param trainingFilePath The path to the training file. The file must be in a tab (\t) separated column column
     *            format where the first column is the term and the second column is the concept.
     * @param testFilePath The path to the test file. The file must be in a tab (\t) separated column column
     *            format where the first column is the term and the second column is the concept.
     * @param minNumberOfSeedsPerConcept The minimal number of training seeds per concept.
     * @param maxNumberOfSeedsPerConcept The maximal number of training seeds per concept.
     */
    public void evaluateSeedInputOnly(String trainingFilePath, String testFilePath, int minNumberOfSeedsPerConcept,
            int maxNumberOfSeedsPerConcept) {

        StopWatch stopWatch = new StopWatch();

        // evaluate in both modes, English and language independent
        for (int i = 0; i < 2; i++) {
            LanguageMode mode;
            if (i == 0) {
                mode = LanguageMode.English;
            } else {
                mode = LanguageMode.LanguageIndependent;
            }

            LOGGER.info("start evaluating in " + mode + " mode");

            StringBuilder results = new StringBuilder();

            results.append("TUDNER, mode = ").append(mode).append("\n");
            results.append(";All;;;;;;Unseen only;;;;;;\n");
            results.append("Number of Seeds;Exact Precision;Exact Recall;Exact F1;MUC Precision;MUC Recall;MUC F1;Exact Precision;Exact Recall;Exact F1;MUC Precision;MUC Recall;MUC F1;\n");

            for (int j = minNumberOfSeedsPerConcept; j <= maxNumberOfSeedsPerConcept; j++) {

                LOGGER.info("evaluating with " + j + " seed entities");

                PalladianNer tagger = new PalladianNer(mode, TrainingMode.Sparse);

                Annotations<ContextAnnotation> annotations = FileFormatParser
                        .getSeedAnnotations(trainingFilePath, j);

                LOGGER.info("train on these annotations: " + annotations);

                // train the tagger using seed annotations only
                String modelPath = EVALUATION_PATH + "tudner_seedOnlyEvaluation_" + j + "Seeds_" + mode + "."
                        + tagger.getModelFileEndingIfNotSetAutomatically();
                tagger.train(annotations, modelPath);

                // evaluate over complete test set (k=0) and on unseen entities only (k=1)
                for (int k = 0; k < 2; k++) {

                    // load the trained model
                    tagger = new PalladianNer(mode);
                    tagger.loadModel(modelPath);
                    EvaluationResult er;

                    if (k == 0) {
                        er = tagger.evaluate(testFilePath, TaggingFormat.COLUMN);
                    } else {
                        er = tagger.evaluate(testFilePath, TaggingFormat.COLUMN, getValues(annotations));
                    }

                    // write the result line
                    if (k == 0) {
                        results.append(j).append(";");
                    }

                    results.append(er.getPrecision(EvaluationMode.EXACT_MATCH)).append(";");
                    results.append(er.getRecall(EvaluationMode.EXACT_MATCH)).append(";");
                    results.append(er.getF1(EvaluationMode.EXACT_MATCH)).append(";");
                    results.append(er.getPrecision(EvaluationMode.MUC)).append(";");
                    results.append(er.getRecall(EvaluationMode.MUC)).append(";");
                    results.append(er.getF1(EvaluationMode.MUC)).append(";");

                    if (k > 0) {
                        results.append("\n");
                    }

                }

            }

            FileHelper.writeToFile(EVALUATION_PATH + "evaluateSeedInputOnlyNER_" + mode + ".csv", results);

            LOGGER.info("evaluated TUDNER in " + mode + " mode in " + stopWatch.getElapsedTimeString());
        }

    }

    private Set<String> getValues(List<? extends Annotation> annotations) {
        Set<String> values = CollectionHelper.newHashSet();
        for (Annotation annotation : annotations) {
            values.add(annotation.getValue());
        }
        return values;
    }

    /**
     * <p>
     * Evaluate a given named entity recognizer on one dataset. Train and test the NER on minDocuments up to
     * maxDocuments and write the results in a csv which will be saved to dependencyOnTrainingSetSize_X.csv where X is
     * the name of the NER.
     * </p>
     * <p>
     * This evaluation method will generate one csv file in the following format:
     * 
     * <pre>
     *                      |                     Complete Test Set                                     |                           Unseen entities only
     * Number of Documents  | Ex. Precision | Ex. Recall | Ex. F1 | MUC Precision | MUC Recall | MUC F1 | Ex. Precision | Ex. Recall | Ex. F1 | MUC Precision | MUC Recall | MUC F1
     * minDocuments         |
     * ...                  |
     * maxDocuments         |
     * </pre>
     * 
     * The result file will be written to EVALUATION_PATH.
     * </p>
     * 
     * @param tagger The named entity recognizer that should be evaluated.
     * @param trainingFilePath The path to the training file that contains all documents.
     * @param testFilePath The path to the test file on which the NER should be tested on.
     * @param documentSeparator The separator for the documents in the given file.
     * @param minDocuments The minimal number of documents to consider.
     * @param maxDocuments The maximal number of documents to consider.
     * @param stepSize The size of the steps between minDocuments and maxDocuments.
     */
    public void evaluateDependencyOnTrainingSetSize(TrainableNamedEntityRecognizer tagger, String trainingFilePath,
            String testFilePath, String documentSeparator, int minDocuments, int maxDocuments, int stepSize) {

        StopWatch stopWatch = new StopWatch();

        LOGGER.info("evaluate " + tagger.getName() + " on " + testFilePath + " with " + minDocuments + " to "
                + maxDocuments + " documents with step size" + stepSize);

        // split the training set in a number of files containing the documents
        DatasetProcessor processor = new DatasetProcessor();
        List<String> splitFilePaths = processor.splitFile(trainingFilePath, documentSeparator, minDocuments,
                maxDocuments, stepSize);

        StringBuilder results = new StringBuilder();

        for (String filePath : splitFilePaths) {

            stopWatch.start();

            LOGGER.info("train " + tagger.getName() + " on " + filePath);

            String numberOfDocuments = StringHelper.getSubstringBetween(filePath, "_sep_", ".");

            // get the annotations
            Annotations<ContextAnnotation> annotations = FileFormatParser.getSeedAnnotations(filePath, -1);

            String modelFilePath = EVALUATION_PATH + tagger.getName() + "_nerModel_" + numberOfDocuments + "."
                    + tagger.getModelFileEndingIfNotSetAutomatically();
            tagger.train(filePath, modelFilePath);

            // evaluate over complete test set (k=0) and on unseen entities only (k=1)

            for (int k = 0; k < 2; k++) {

                EvaluationResult er;

                if (k == 0) {
                    er = tagger.evaluate(testFilePath, TaggingFormat.COLUMN);
                } else {
                    er = tagger.evaluate(testFilePath, TaggingFormat.COLUMN, getValues(annotations));
                }

                // write the result line
                if (k == 0) {
                    results.append(numberOfDocuments).append(";");
                }

                results.append(er.getPrecision(EvaluationMode.EXACT_MATCH)).append(";");
                results.append(er.getRecall(EvaluationMode.EXACT_MATCH)).append(";");
                results.append(er.getF1(EvaluationMode.EXACT_MATCH)).append(";");
                results.append(er.getPrecision(EvaluationMode.MUC)).append(";");
                results.append(er.getRecall(EvaluationMode.MUC)).append(";");
                results.append(er.getF1(EvaluationMode.MUC)).append(";");

                if (k > 0) {
                    results.append("\n");
                }

            }

            LOGGER.info("evaluated " + tagger.getName() + " on " + numberOfDocuments + " documents in "
                    + stopWatch.getTotalElapsedTimeString());

            FileHelper.writeToFile(EVALUATION_PATH + "dependencyOnTrainingSetSize_" + tagger.getName() + ".csv",
                    results);

            // er.
            // FileHelper.writeToFile(EVALUATION_PATH + "evaluatePerConcept_" + tagger.getName() + "_" +
            // numberOfDocuments
            // + ".csv", results);
        }

        FileHelper.writeToFile(EVALUATION_PATH + "dependencyOnTrainingSetSize_" + tagger.getName() + ".csv", results);
    }

    /**
     * <p>
     * Evaluate a given named entity recognizer on one dataset. Print the performance per concept.
     * </p>
     * <p>
     * This evaluation method will generate one csv file in the following format:
     * 
     * <pre>
     *                      |                     Complete Test Set                                     |                           Unseen entities only
     * Concept              | Ex. Precision | Ex. Recall | Ex. F1 | MUC Precision | MUC Recall | MUC F1 | Ex. Precision | Ex. Recall | Ex. F1 | MUC Precision | MUC Recall | MUC F1
     * ..                   |
     * ..                   |
     * Averaged             |
     * </pre>
     * 
     * The result file will be written to EVALUATION_PATH.
     * </p>
     * 
     * @param tagger The named entity recognizer that should be evaluated.
     * @param trainingFilePath The path to the training file that contains all documents.
     * @param testFilePath The path to the test file on which the NER should be tested on.
     * @return Return the average performance for the given tagger on the given dataset.
     */
    public String evaluatePerConceptPerformance(TrainableNamedEntityRecognizer tagger, String trainingFilePath,
            String testFilePath, int numberOfSeeds) {

        StopWatch stopWatch = new StopWatch();

        LOGGER.info("start evaluating per concept performance for " + tagger.getName() + " on " + numberOfSeeds
                + " seeds");

        String modelFilePath = EVALUATION_PATH + "evaluatePerConceptModel_" + tagger.getName() + "_" + numberOfSeeds
        + "." + tagger.getModelFileEndingIfNotSetAutomatically();
        tagger.train(trainingFilePath, modelFilePath);
        LOGGER.info("training " + tagger.getName() + " took " + stopWatch.getElapsedTimeString());

        StringBuilder results = new StringBuilder();

        // this is the last line of the results which sums up the performance
        StringBuilder averagedLine = new StringBuilder();

        // get the annotations
        Annotations<ContextAnnotation> annotations = FileFormatParser.getSeedAnnotations(trainingFilePath, -1);

        // evaluate once with complete test set and once only over unseen entities
        stopWatch.start();
        tagger.loadModel(modelFilePath);
        EvaluationResult er1 = tagger.evaluate(testFilePath, TaggingFormat.COLUMN);
        LOGGER.info("evaluating " + tagger.getName() + " on the complete data took " + stopWatch.getElapsedTimeString());

        stopWatch.start();
        EvaluationResult er2 = tagger.evaluate(testFilePath, TaggingFormat.COLUMN, getValues(annotations));
        LOGGER.info("evaluating " + tagger.getName() + " on the unseen data took " + stopWatch.getElapsedTimeString());

        Set<String> concepts = FileFormatParser.getTagsFromColumnFile(trainingFilePath, "\t");
        concepts.remove("O");

        // write head
        results.append(";Complete Testset;;;;;;Unseen Data Only;;;;;\n");
        results.append(";Ex. Prec.;Ex. Rec.;Ex. F1;MUC Prec.;MUC Rec.;MUC F1;Ex. Prec.;Ex. Rec.;Ex. F1;MUC Prec.;MUC Rec.;MUC F1\n");

        for (String concept : concepts) {

            // evaluate over complete test set (k=0) and on unseen entities only (k=1)
            for (int k = 0; k < 2; k++) {

                EvaluationResult er = null;

                if (k == 0) {
                    er = er1;
                } else {
                    er = er2;
                }

                // write the result line
                if (k == 0) {
                    results.append(concept).append(";");
                }

                results.append(er.getPrecisionFor(concept, EvaluationMode.EXACT_MATCH)).append(";");
                results.append(er.getRecallFor(concept, EvaluationMode.EXACT_MATCH)).append(";");
                results.append(er.getF1For(concept, EvaluationMode.EXACT_MATCH)).append(";");
                results.append(er.getPrecisionFor(concept, EvaluationMode.MUC)).append(";");
                results.append(er.getRecallFor(concept, EvaluationMode.MUC)).append(";");
                results.append(er.getF1For(concept, EvaluationMode.MUC)).append(";");

                if (k > 0) {
                    results.append("\n");
                }

            }

        }

        // evaluate over complete test set (k=0) and on unseen entities only (k=1)
        for (int k = 0; k < 2; k++) {

            EvaluationResult er = null;

            if (k == 0) {
                er = er1;
            } else {
                er = er2;
            }

            if (k == 0) {
                results.append("Averaged").append(";");
            }

            averagedLine.append(er.getPrecision(EvaluationMode.EXACT_MATCH)).append(";");
            averagedLine.append(er.getRecall(EvaluationMode.EXACT_MATCH)).append(";");
            averagedLine.append(er.getF1(EvaluationMode.EXACT_MATCH)).append(";");
            averagedLine.append(er.getPrecision(EvaluationMode.MUC)).append(";");
            averagedLine.append(er.getRecall(EvaluationMode.MUC)).append(";");
            averagedLine.append(er.getF1(EvaluationMode.MUC)).append(";");


        }

        results.append(averagedLine);

        FileHelper.writeToFile(EVALUATION_PATH + "evaluatePerConcept_" + tagger.getName() + "_" + numberOfSeeds
                + ".csv", results);

        LOGGER.info("evaluated " + tagger.getName() + " on " + trainingFilePath + " in "
                + stopWatch.getTotalElapsedTimeString());

        return averagedLine.toString();
    }

    public void evaluateOnGeneratedTrainingset(List<TrainableNamedEntityRecognizer> taggers, String targetFolder,
            String testFilePath) {

        StopWatch stopWatch = new StopWatch();

        StringBuilder results = new StringBuilder();

        // count the numer of text files for evaluation in the target folder
        int maxNumberOfSeeds = FileHelper.getFiles(targetFolder, "seedsTest").length;

        // write the result head
        results.append(";");
        for (NamedEntityRecognizer tagger : taggers) {
            results.append(tagger.getName()).append(";;;;;;;;;;;;");
        }
        results.append("\n");
        results.append(";Complete Testset;;;;;;Unseen Data Only;;;;;\n");
        results.append(";Ex. Prec.;Ex. Rec.;Ex. F1;MUC Prec.;MUC Rec.;MUC F1;Ex. Prec.;Ex. Rec.;Ex. F1;MUC Prec.;MUC Rec.;MUC F1\n");

        for (int i = 1; i <= maxNumberOfSeeds; i++) {
            LOGGER.info("start evaluating on generated training data using " + i + " seeds");

            results.append(i).append(";");

            String generatedTrainingFilePath = targetFolder + "seedsTest" + i + ".txt";

            for (TrainableNamedEntityRecognizer tagger : taggers) {
                results.append(evaluatePerConceptPerformance(tagger, generatedTrainingFilePath, testFilePath, i));
            }

            results.append("\n");
        }

        FileHelper.writeToFile(EVALUATION_PATH + "autoGeneratedTests.txt", results);

        LOGGER.info("finished evaluating " + taggers.size() + " NERs using generated training data in "
                + stopWatch.getTotalElapsedTimeString());
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // DatasetCreator.generateDatasets("data/temp/autoGeneratedDataConll2/", "www",
        // "data/datasets/ner/conll/training.txt", 100, 100, 10);

        // FileFormatParser.xmlToColumn("data/datasets/ner/tud/manuallyPickedSeeds/seedList2.xml","data/datasets/ner/tud/manuallyPickedSeeds/seedListC.txt",
        // "\t");
        // System.exit(0);
        //
        // DatasetCreator.generateDatasets("data/temp/autoGeneratedDataTUD5/", "www",
        // "data/datasets/ner/tud/manuallyPickedSeeds/seedListC.txt", 100, 100, 10);
        // System.exit(0);

        List<TrainableNamedEntityRecognizer> taggerList = new ArrayList<TrainableNamedEntityRecognizer>();
        // taggerList.add(new StanfordNER());
        // IllinoisLbjNer lbjNer = new IllinoisLbjNer();
        taggerList.add(new PalladianNer(LanguageMode.English));
        taggerList.add(new PalladianNer(LanguageMode.LanguageIndependent));
        // lbjNer.setConllEvaluation(true); // you have to set conllEvaluation to true if used for conll
        // taggerList.add(lbjNer);
        // taggerList.add(new LingPipeNER());
        OpenNlpNer openNlpNer = new OpenNlpNer();
        openNlpNer.setConllEvaluation(true); // you have to set conllEvaluation to true if used for conll
        // taggerList.add(openNlpNer);
        // taggerList.add(new JulieNER());

        // String conll2003TrainingPath = "data/datasets/ner/conll/training_verysmall.txt";
        // String conll2003TestPath = "data/datasets/ner/conll/test_validation_small.txt";

        // ////////////////////////// evaluate on CoNLL 2003 data ////////////////////////////
        String conll2003TrainingPath = "data/datasets/ner/conll/training.txt";
        String conll2003TestPath = "data/datasets/ner/conll/test_final.txt";

        String tud2011TrainingPath = "data/datasets/ner/tud/tud2011_train.txt";
        String tud2011TestPath = "data/datasets/ner/tud/tud2011_test.txt";

        Evaluator evaluator = new Evaluator();

        // evaluate using seed entities only (only TUDNER)
        // evaluator.evaluateSeedInputOnly(conll2003TrainingPath, conll2003TestPath, 1, 50);

        // evaluate all tagger how they depend on the number of documents in the training set
        for (TrainableNamedEntityRecognizer tagger : taggerList) {
            evaluator.evaluatePerConceptPerformance(tagger, conll2003TrainingPath, conll2003TestPath, 0);
            // evaluator.evaluatePerConceptPerformance(tagger, tud2011TrainingPath, tud2011TestPath, 0);
            // evaluator.evaluateDependencyOnTrainingSetSize(tagger, conll2003TrainingPath, conll2003TestPath,
            // "=-DOCSTART-\tO", 1, 501, 10);
        }
        System.exit(0);

        // List<NamedEntityRecognizer> taggerListWebLearning = new ArrayList<NamedEntityRecognizer>();
        // TUDNER tudnerForIncompleteTraining = new TUDNER(Mode.English);
        // tudnerForIncompleteTraining.setTrainingMode(TrainingMode.Incomplete);
        //
        // taggerListWebLearning.add(tudnerForIncompleteTraining);
        // evaluator.evaluateOnGeneratedTrainingset(taggerListWebLearning, "data/temp/autoGeneration/",
        // conll2003TestPath);

        // ////////////////////////// evaluate on TUD 2011 data ////////////////////////////

        // evaluate using seed entities only (only TUDNER)
        // evaluator.evaluateSeedInputOnly(tud2011TrainingPath, tud2011TestPath, 1, 50);

        // evaluate all tagger how they depend on the number of documents in the training set
        taggerList.clear();
        taggerList.add(new PalladianNer(LanguageMode.English));
        taggerList.add(new PalladianNer(LanguageMode.LanguageIndependent));
        for (TrainableNamedEntityRecognizer tagger : taggerList) {
            // evaluator.evaluatePerConceptPerformance(tagger, tud2011TrainingPath, tud2011TestPath, 0);
            evaluator.evaluateDependencyOnTrainingSetSize(tagger, tud2011TrainingPath, tud2011TestPath,
                    "=-DOCSTART-\tO", 1, 61, 5);
        }
        System.exit(0);

        // List<NamedEntityRecognizer> taggerListWebLearning = new ArrayList<NamedEntityRecognizer>();
        // PalladianNer tudnerForIncompleteTraining = new PalladianNer(LanguageMode.English);
        // tudnerForIncompleteTraining.setTrainingMode(TrainingMode.Sparse);
        //
        // taggerListWebLearning.add(tudnerForIncompleteTraining);
        // evaluator.evaluateOnGeneratedTrainingset(taggerListWebLearning, "data/temp/autoGeneratedDataTUD3/",
        // tud2011TestPath);

    }

}
