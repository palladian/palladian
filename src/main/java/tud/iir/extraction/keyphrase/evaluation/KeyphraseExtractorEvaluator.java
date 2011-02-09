package tud.iir.extraction.keyphrase.evaluation;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import tud.iir.classification.page.evaluation.Dataset;
import tud.iir.classification.page.evaluation.TrainingDataSeparation;
import tud.iir.extraction.keyphrase.Keyphrase;
import tud.iir.extraction.keyphrase.KeyphraseExtractor;
import tud.iir.extraction.keyphrase.PalladianKeyphraseExtractor;
import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.StopWatch;

public class KeyphraseExtractorEvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(KeyphraseExtractorEvaluator.class);

    /** The stemmer is needed to compare the assigned tags to the existing ones. */
    private SnowballStemmer stemmer = new englishStemmer();

    // private KeyphraseExtractor extractor;

    private List<KeyphraseExtractor> extractors = new ArrayList<KeyphraseExtractor>();

    private static final String TEMP_TRAINING_DATA = "data/temp/KeyphraseExtractorEvaluation_train.txt";

    private static final String TEMP_TESTING_DATA = "data/temp/KeyphraseExtractorEvaluation_test.txt";

    private static final String EVALUATION_RESULT = "data/temp/KeyphraseExtractorEvaluation_result.txt";

    // public KeyphraseExtractorEvaluator(KeyphraseExtractor extractor) {
    // this.extractor = extractor;
    // }

    public void addExtractor(KeyphraseExtractor extractor) {
        extractors.add(extractor);
    }

    public void evaluate(Dataset dataset, int repeats) {
        for (int i = 0; i < repeats; i++)
            for (KeyphraseExtractor extractor : extractors) {
                evaluate(extractor, dataset);
            }
    }

    public void evaluate(KeyphraseExtractor extractor, Dataset dataset) {
        evaluate(extractor, dataset, -1, false);
    }

    public void evaluate(KeyphraseExtractor extractor, Dataset dataset, int testLimit, boolean useExistingTestData) {

        LOGGER.info("evaluating " + extractor.getExtractorName() + " with " + dataset);
        StopWatch sw = new StopWatch();

        if (!useExistingTestData) {
            createTrainTestData(dataset);
        }

        // train, if applicable
        if (extractor.needsTraining()) {
            Dataset trainingDataset = new Dataset();
            trainingDataset.setPath(TEMP_TRAINING_DATA);
            trainingDataset.setRootPath(dataset.getRootPath());
            trainingDataset.setSeparationString(dataset.getSeparationString());
            trainingDataset.setFirstFieldLink(dataset.isFirstFieldLink());
            extractor.train(trainingDataset);
        }

        // testing set
        Dataset testingDataset = new Dataset();
        testingDataset.setPath(TEMP_TESTING_DATA);
        testingDataset.setRootPath(dataset.getRootPath());
        testingDataset.setSeparationString(dataset.getSeparationString());
        testingDataset.setFirstFieldLink(dataset.isFirstFieldLink());

        // evaluation
        ControlledTaggerEvaluationResult result = test(extractor, testingDataset, testLimit);

        // write result file
        try {
            FileHelper.appendFile(EVALUATION_RESULT, result.toString() + "\n");
        } catch (IOException e) {
            LOGGER.error(e);
        }

        LOGGER.info("finished evaluation in " + sw.getElapsedTimeString());

    }

    public void createTrainTestData(Dataset dataset) {

        TrainingDataSeparation separation = new TrainingDataSeparation();

        String fileToSeparate = dataset.getPath();
        int trainingDataPercentage = dataset.getUsePercentTraining();
        boolean randomlyChooseLines = true;
        // boolean randomlyChooseLines = false; // XXX

        try {
            separation.separateFile(fileToSeparate, TEMP_TRAINING_DATA, TEMP_TESTING_DATA, trainingDataPercentage,
                    randomlyChooseLines);
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        }

    }

    public ControlledTaggerEvaluationResult test(final KeyphraseExtractor extractor, final Dataset dataset,
            final int limit) {

        LOGGER.info("testing ...");

        final ControlledTaggerEvaluationResult evaluationResult = new ControlledTaggerEvaluationResult();
        extractor.startExtraction();

        StopWatch sw = new StopWatch();

        FileHelper.performActionOnEveryLine(dataset.getPath(), new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] split = line.split(dataset.getSeparationString());

                if (split.length < 2) {
                    return;
                }

                // the manually assigned keyphrases
                Set<String> realKeyphrases = new HashSet<String>();
                for (int i = 1; i < split.length; i++) {
                    realKeyphrases.add(split[i].toLowerCase());
                }
                int realCount = realKeyphrases.size();
                Set<String> stemmedRealKeyphrases = stem(realKeyphrases);
                stemmedRealKeyphrases.addAll(realKeyphrases);

                // get the text; either directly from the dataset file or from the provided link,
                // depending of the Dataset settings
                String text = split[0];
                if (dataset.isFirstFieldLink()) {
                    text = FileHelper.readFileToString(dataset.getRootPath() + "/" + split[0]);
                }

                // automatically extract keyphrases
                List<Keyphrase> assignedKeyphrases = extractor.extract(text);
                int correctCount = 0;
                int assignedCount = assignedKeyphrases.size();

                // determine Pr/Rc values by considering assigned and real keyphrases
                for (Keyphrase assigned : assignedKeyphrases) {
                    for (String real : stemmedRealKeyphrases) {

                        boolean correct = real.equalsIgnoreCase(assigned.getValue());
                        correct = correct || real.equalsIgnoreCase(assigned.getValue().replace(" ", ""));
                        correct = correct || real.equalsIgnoreCase(stem(assigned.getValue()));
                        correct = correct || real.equalsIgnoreCase(stem(assigned.getValue().replace(" ", "")));

                        if (correct) {
                            correctCount++;
                            break; // inner loop
                        }
                    }
                }

                float precision = (float) correctCount / assignedCount;
                if (Float.isNaN(precision)) {
                    precision = 0;
                }
                float recall = (float) correctCount / realCount;

                LOGGER.info("real keyphrases: " + realKeyphrases);
                LOGGER.info("assigned keyphrases: " + assignedKeyphrases);
                LOGGER.info("real: " + realCount + " assigned: " + assignedCount + " correct: " + correctCount);
                LOGGER.info("pr: " + precision + " rc: " + recall);
                LOGGER.info("----------------------------------------------------------");

                evaluationResult.addTestResult(precision, recall, assignedCount);

                if (evaluationResult.getTaggedEntryCount() == limit) {
                    breakLineLoop();
                }

            }
        });

        evaluationResult.printStatistics();
        LOGGER.info("finished evaluation in " + sw.getElapsedTimeString());

        return evaluationResult;

    }

    public ControlledTaggerEvaluationResult test(KeyphraseExtractor extractor, Dataset dataset) {
        return test(extractor, dataset, -1);
    }

    public ControlledTaggerEvaluationResult test(KeyphraseExtractor extractor, String filePath, final int limit) {

        Dataset dataset = new Dataset();
        dataset.setFirstFieldLink(false);
        dataset.setSeparationString("#");

        ControlledTaggerEvaluationResult evaluationResult = test(extractor, dataset, limit);

        return evaluationResult;

    }

    // public ControlledTaggerEvaluationResult test() {
    // return test(TEMP_TESTING_DATA, -1);
    // }

    private String stem(String unstemmed) {

        StringBuilder sb = new StringBuilder();

        // stem each part of the phrase
        String[] parts = unstemmed.toLowerCase().split(" ");
        for (String part : parts) {
            stemmer.setCurrent(part);
            stemmer.stem();
            sb.append(stemmer.getCurrent());
        }

        return sb.toString();

    }

    private Set<String> stem(Set<String> unstemmed) {
        Set<String> result = new HashSet<String>();
        for (String unstemmedTag : unstemmed) {
            String stem = stem(unstemmedTag);
            result.add(stem);
        }
        return result;
    }

    public static void main(String[] args) {

        KeyphraseExtractor keyphraseExtractor;
        PalladianKeyphraseExtractor palladianKeyphraseExtractor = new PalladianKeyphraseExtractor();
        // keyphraseExtractor = new AlchemyKeywordExtraction();
        // keyphraseExtractor = new ControlledTagger();
        // keyphraseExtractor = new FiveFiltersTermExtraction();
        // keyphraseExtractor = new YahooTermExtraction();
        palladianKeyphraseExtractor.getSettings().setModelPath("data/temp/PalladianWithPOS");
        
        keyphraseExtractor = palladianKeyphraseExtractor;

        KeyphraseExtractorEvaluator evaluator = new KeyphraseExtractorEvaluator();

        // evaluator.addExtractor(palladianKeyphraseExtractor);
        // evaluator.addExtractor(controlledTagger);

        Dataset dataset = new Dataset();
        // dataset.setPath("/Users/pk/temp/citeulike180/citeulike180index.txt");
        // dataset.setRootPath("/Users/pk/temp/citeulike180/documents/");
        // dataset.setPath("/Users/pk/temp/fao780.txt");
        // dataset.setRootPath("/Users/pk/temp/fao780/");

        // dataset.setSeparationString("#");

        dataset.setPath("/home/pk/temp/deliciousT140/deliciousT140index.txt");
        dataset.setRootPath("/home/pk/temp/deliciousT140/docs");
        
        // dataset.setPath("/Users/pk/temp/deliciousT140/deliciousT140index.txt");
        // dataset.setRootPath("/Users/pk/temp/deliciousT140/docs");
        dataset.setSeparationString(" ");
        dataset.setFirstFieldLink(true);
        dataset.setUsePercentTraining(50);

        // evaluator.evaluate(keyphraseExtractor, dataset, -1, true);
        evaluator.evaluate(keyphraseExtractor, dataset, 1000, true);

    }

}
