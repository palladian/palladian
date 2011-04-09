package ws.palladian.extraction.entity.ner.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.extraction.entity.ner.Annotation;
import ws.palladian.extraction.entity.ner.Annotations;
import ws.palladian.extraction.entity.ner.FileFormatParser;
import ws.palladian.extraction.entity.ner.NamedEntityRecognizer;
import ws.palladian.extraction.entity.ner.TaggingFormat;
import ws.palladian.extraction.entity.ner.dataset.DatasetProcessor;
import ws.palladian.extraction.entity.ner.tagger.IllinoisLbjNER;
import ws.palladian.extraction.entity.ner.tagger.JulieNER;
import ws.palladian.extraction.entity.ner.tagger.LingPipeNER;
import ws.palladian.extraction.entity.ner.tagger.OpenNLPNER;
import ws.palladian.extraction.entity.ner.tagger.StanfordNER;
import ws.palladian.extraction.entity.ner.tagger.TUDNER;
import ws.palladian.extraction.entity.ner.tagger.TUDNER.Mode;
import ws.palladian.helper.DataHolder;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.nlp.StringHelper;

/**
 * @author David
 * 
 */
/**
 * @author David
 * 
 */
public class Evaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Evaluator.class);

    /**
     * <p>
     * Seed-only evaluation. In this evaluation scenario, the NER is only trained on a set of seed entities per concept.
     * Most NERs can not be trained in this fashion so we only evaluate {@link TUDNER}.
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
     * be written to data/temp/nerEvaluation/.
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
            Mode mode;
            if (i == 0) {
                mode = Mode.English;
            } else {
                mode = Mode.LanguageIndependent;
            }
            
            LOGGER.info("start evaluating in mode " + mode);
            
            StringBuilder results = new StringBuilder();            
            
            results.append("TUDNER, mode = ").append(mode).append("\n");
            results.append("Number of Seeds;Exact Precision;Exact Recall;Exact F1;MUC Precision;MUC Recall;MUC F1;\n");

            for (int j = minNumberOfSeedsPerConcept; j <= maxNumberOfSeedsPerConcept; j++) {

                TUDNER tagger = new TUDNER();
                tagger.setMode(mode);

                Annotations annotations = (Annotations) DataHolder.getInstance().getDataObject("annotations");
                if (annotations == null) {
                    annotations = getSeedAnnotations(trainingFilePath, j);
                    DataHolder.getInstance().putDataObject("annotations", annotations);
                }

                // train the tagger using seed annotations only
                tagger.train(annotations, "data/temp/nerEvaluation/tudner_seedOnlyEvaluation.model");

                // load the trained model
                tagger = new TUDNER();
                tagger.setMode(mode);

                // evaluate over complete test set (k=0) and on unseen entities only (k=1)
                for (int k = 0; k < 2; k++) {

                    EvaluationResult er = null;

                    if (k == 0) {
                        er = tagger.evaluate(testFilePath, "data/temp/nerEvaluation/tudner_seedOnlyEvaluation.model",
                                TaggingFormat.COLUMN);
                    } else {
                        er = tagger.evaluate(testFilePath, "data/temp/nerEvaluation/tudner_seedOnlyEvaluation.model",
                                TaggingFormat.COLUMN, annotations);
                    }

                    // write the result line
                    if (k == 0) {
                        results.append(j).append(";");
                    }

                    results.append(er.getPrecision(EvaluationResult.EXACT_MATCH)).append(";");
                    results.append(er.getRecall(EvaluationResult.EXACT_MATCH)).append(";");
                    results.append(er.getF1(EvaluationResult.EXACT_MATCH)).append(";");
                    results.append(er.getPrecision(EvaluationResult.MUC)).append(";");
                    results.append(er.getRecall(EvaluationResult.MUC)).append(";");
                    results.append(er.getF1(EvaluationResult.MUC)).append(";");

                    if (k > 0) {
                        results.append("\n");
                    }

                }

            }
            
            FileHelper.writeToFile("data/temp/nerEvaluation/evaluateSeedInputOnlyNER_" + mode + ".csv", results);
            
            LOGGER.info("evaluated TUDNER in " + mode + " mode in " + stopWatch.getElapsedTimeString());
        }
        
    }

    /**
     * Get a list of annotations from a tagged file.
     * 
     * @param annotatedFilePath The path to the tagged file. The file must be in a tab (\t) separated column column
     *            format where the first column is the term and the second column is the concept.
     * @param numberOfSeedsPerConcept The number of annotations that have to be found for each concept.
     * @return Annotations with numberOfSeedsPerConcept entries per concept.
     */
    private Annotations getSeedAnnotations(String annotatedFilePath, int numberOfSeedsPerConcept) {
        Annotations annotations = new Annotations();

        // count the number of collected seeds per concept
        CountMap conceptSeedCount = new CountMap();

        Annotations allAnnotations = FileFormatParser.getAnnotationsFromColumn(annotatedFilePath);

        // iterate through the annotations and collect numberOfSeedsPerConcept
        for (Annotation annotation : allAnnotations) {

            String conceptName = annotation.getInstanceCategoryName();
            int numberOfSeeds = conceptSeedCount.get(conceptName);

            if (numberOfSeeds < numberOfSeedsPerConcept) {
                annotations.add(annotation);
                conceptSeedCount.increment(conceptName);
            }

        }

        return annotations;
    }

    /**
     * Evaluate a given named entity recognizer on one dataset. Train and test the NER on minDocuments up to
     * maxDocuments and write the results in a csv which will be saved to dependencyOnTrainingSetSize_X.csv where X is
     * the name of the NER.
     * 
     * @param tagger The named entity recognizer that should be evaluated.
     * @param trainingFilePath The path to the training file that contains all documents.
     * @param testFilePath The path to the test file on which the NER should be tested on.
     * @param documentSeparator The separator for the documents in the given file.
     * @param minDocuments The minimal number of documents to consider.
     * @param maxDocuments The maximal number of documents to consider.
     */
    public void evaluateDependencyOnTrainingSetSize(NamedEntityRecognizer tagger, String trainingFilePath,
            String testFilePath,
            String documentSeparator, int minDocuments, int maxDocuments) {

        // split the training set in a number of files containing the documents
        DatasetProcessor processor = new DatasetProcessor();
        List<File> splitFiles = processor.splitFile(trainingFilePath, documentSeparator, minDocuments, maxDocuments);

        StopWatch stopWatch = new StopWatch();

        StringBuilder results = new StringBuilder();

        for (File file : splitFiles) {

            stopWatch.start();

            String modelFilePath = "data/temp/nerEvaluation/nerModel" + tagger.getModelFileEnding();
            tagger.train(file.getPath(), modelFilePath);

            EvaluationResult er = tagger.evaluate("data/datasets/ner/conll/test_final.txt",
 modelFilePath,
                    TaggingFormat.COLUMN);

            results.append(er.getPrecision(EvaluationResult.EXACT_MATCH)).append(";");
            results.append(er.getRecall(EvaluationResult.EXACT_MATCH)).append(";");
            results.append(er.getF1(EvaluationResult.EXACT_MATCH)).append(";");
            results.append(er.getPrecision(EvaluationResult.MUC)).append(";");
            results.append(er.getRecall(EvaluationResult.MUC)).append(";");
            results.append(er.getF1(EvaluationResult.MUC)).append(";");

            LOGGER.info("evaluated " + tagger.getName() + " on "
                    + StringHelper.getSubstringBetween(file.getName(), "_sep_", ".") + " documents in "
                    + stopWatch.getElapsedTimeString());
        }

        FileHelper.writeToFile("data/temp/nerEvaluation/dependencyOnTrainingSetSize_" + tagger.getName() + ".csv",
                results);
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        List<NamedEntityRecognizer> taggerList = new ArrayList<NamedEntityRecognizer>();
        taggerList.add(new StanfordNER());
        taggerList.add(new IllinoisLbjNER());
        taggerList.add(new JulieNER());
        taggerList.add(new LingPipeNER());
        taggerList.add(new OpenNLPNER());
        taggerList.add(new TUDNER(Mode.English));
        taggerList.add(new TUDNER(Mode.LanguageIndependent));

        String conll2003TrainingPath = "data/datasets/ner/conll/training.txt";
        String conll2003TestPath = "data/datasets/ner/conll/test_final.txt";

        Evaluator evaluator = new Evaluator();

        // evaluate using seed entities only (only TUDNER)
        evaluator.evaluateSeedInputOnly(conll2003TrainingPath, conll2003TestPath, 1, 50);

        // evaluate all tagger how they depend on the number of documents in the training set
        for (NamedEntityRecognizer tagger : taggerList) {
            evaluator.evaluateDependencyOnTrainingSetSize(tagger, conll2003TrainingPath, conll2003TestPath,
                    "=-DOCSTART-\tO", 1, 50);
        }

    }

}
