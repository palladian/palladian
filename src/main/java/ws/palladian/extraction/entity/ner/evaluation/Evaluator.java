package ws.palladian.extraction.entity.ner.evaluation;

import org.apache.log4j.Logger;

import ws.palladian.extraction.entity.ner.Annotation;
import ws.palladian.extraction.entity.ner.Annotations;
import ws.palladian.extraction.entity.ner.FileFormatParser;
import ws.palladian.extraction.entity.ner.TaggingFormat;
import ws.palladian.extraction.entity.ner.tagger.TUDNER;
import ws.palladian.extraction.entity.ner.tagger.TUDNER.Mode;
import ws.palladian.helper.DataHolder;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CountMap;

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
     * be written to data/temp/.
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
                tagger.train(annotations, "data/temp/tudner_seedOnlyEvaluation.model");

                // load the trained model
                tagger = new TUDNER();
                tagger.setMode(mode);

                // evaluate over complete test set (k=0) and on unseen entities only (k=1)
                for (int k = 0; k < 2; k++) {

                    EvaluationResult er = null;

                    if (k == 0) {
                        er = tagger.evaluate(testFilePath, "data/temp/tudner_seedOnlyEvaluation.model",
                                TaggingFormat.COLUMN);
                    } else {
                        er = tagger.evaluate(testFilePath, "data/temp/tudner_seedOnlyEvaluation.model",
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
            
            FileHelper.writeToFile("data/temp/evaluateSeedInputOnlyNER_" + mode + ".csv", results);
            
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
     * @param args
     */
    public static void main(String[] args) {
        Evaluator evaluator = new Evaluator();
        evaluator.evaluateSeedInputOnly("data/datasets/ner/conll/training.txt",
                "data/datasets/ner/conll/test_final.txt", 1, 50);
    }

}
