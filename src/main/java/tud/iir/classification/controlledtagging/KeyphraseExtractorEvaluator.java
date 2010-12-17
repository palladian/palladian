package tud.iir.classification.controlledtagging;

import java.util.HashSet;
import java.util.Set;

import org.apache.log4j.Logger;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import tud.iir.classification.page.evaluation.Dataset;
import tud.iir.extraction.keyphrase.AbstractKeyphraseExtractor;
import tud.iir.extraction.keyphrase.AlchemyKeywordExtraction;
import tud.iir.extraction.keyphrase.FiveFiltersTermExtraction;
import tud.iir.extraction.keyphrase.Keyphrase;
import tud.iir.extraction.keyphrase.MauiKeyphraseExtractor;
import tud.iir.extraction.keyphrase.YahooTermExtraction;
import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.StopWatch;

public class KeyphraseExtractorEvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(KeyphraseExtractorEvaluator.class);

    /** The stemmer is needed to compare the assigned tags to the existing ones. */
    private SnowballStemmer stemmer = new englishStemmer();

    private AbstractKeyphraseExtractor extractor;

    public KeyphraseExtractorEvaluator(AbstractKeyphraseExtractor extractor) {
        this.extractor = extractor;
    }

    public ControlledTaggerEvaluationResult test(final Dataset dataset, final int limit) {

        LOGGER.info("starting evaluation ...");

        final ControlledTaggerEvaluationResult evaluationResult = new ControlledTaggerEvaluationResult();

        StopWatch sw = new StopWatch();
        // final Counter counter = new Counter();
        // final float[] prRcValues = new float[2];

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
                Set<String> stemmedRealKeyphrases = stem(realKeyphrases);
                int realCount = stemmedRealKeyphrases.size();
                realKeyphrases.addAll(stemmedRealKeyphrases);

                // get the text; either directly from the dataset file or from the provided link,
                // depending of the Dataset settings
                String text = split[0];
                if (dataset.isFirstFieldLink()) {
                    text = FileHelper.readFileToString(dataset.getRootPath() + split[0]);
                }

                // automatically extract keyphrases
                Set<Keyphrase> assignedKeyphrases = extractor.extract(text);
                int correctCount = 0;
                int assignedCount = assignedKeyphrases.size();

                // determine Pr/Rc values by considering assigned and real keyphrases
                for (Keyphrase assigned : assignedKeyphrases) {
                    for (String real : realKeyphrases) {

                        // boolean correct = real.equalsIgnoreCase(assigned.getValue());
                        // correct = correct || real.equalsIgnoreCase(assigned.getValue().replace(" ", ""));
                        // correct = correct || real.equalsIgnoreCase(assigned.getStemmedValue());
                        // correct = correct || real.equalsIgnoreCase(assigned.getStemmedValue().replace(" ", ""));
                        // boolean correct = real.equals(stem(assigned.getValue()));

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

                // prRcValues[0] += precision;
                // prRcValues[1] += recall;
                // counter.increment();

                evaluationResult.addTestResult(precision, recall, assignedCount);

                if (evaluationResult.getTaggedEntryCount() == limit) {
                    // if (counter.getCount() == limit) {
                    breakLineLoop();
                }

            }
        });

        // calculate average Pr/Rc/F1 values
        // float averagePrecision = (float) prRcValues[0] / counter.getCount();
        // float averageRecall = (float) prRcValues[1] / counter.getCount();
        // float averageF1 = 2 * averagePrecision * averageRecall / (averagePrecision + averageRecall);

        // LOGGER.info("-----------------------------------------------");
        // LOGGER.info("finished evaluation in " + sw.getElapsedTimeString());
        // LOGGER.info("average precision: " + averagePrecision);
        // LOGGER.info("average recall: " + averageRecall);
        // LOGGER.info("average f1: " + averageF1);

        evaluationResult.printStatistics();
        LOGGER.info("finished evaluation in " + sw.getElapsedTimeString());

        return evaluationResult;

    }

    public ControlledTaggerEvaluationResult test(Dataset dataset) {
        return test(dataset, -1);
    }

    public ControlledTaggerEvaluationResult test(String filePath, final int limit) {

        Dataset dataset = new Dataset();
        dataset.setFirstFieldLink(false);
        dataset.setSeparationString("#");

        ControlledTaggerEvaluationResult evaluationResult = test(dataset, limit);

        return evaluationResult;

    }

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

        // Extractor + Evaluator
        // AbstractKeyphraseExtractor extractor = new PalladianKeyphraseExtractor();
        // AbstractKeyphraseExtractor extractor = new AlchemyKeywordExtraction();
        // AbstractKeyphraseExtractor extractor = new MauiKeyphraseExtractor();
        // AbstractKeyphraseExtractor extractor = new FiveFiltersTermExtraction();
        AbstractKeyphraseExtractor extractor = new YahooTermExtraction();
        
        KeyphraseExtractorEvaluator evaluator = new KeyphraseExtractorEvaluator(extractor);
        

        if (extractor.needsTraining()) {
            // training set
            Dataset trainingDataset = new Dataset();
            trainingDataset.setPath("/home/pk/Desktop/documents/citeulike180splitaa.txt");
            trainingDataset.setSeparationString("#");
            trainingDataset.setFirstFieldLink(true);
            extractor.train(trainingDataset);
        }
        
        // testing set
        Dataset testingDataset = new Dataset();
        testingDataset.setPath("/home/pk/Desktop/documents/citeulike180splitab.txt");
        testingDataset.setSeparationString("#");
        testingDataset.setFirstFieldLink(true);

        // evaluation
        evaluator.test(testingDataset);

    }

}
