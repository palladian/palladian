package ws.palladian.extraction.keyphrase.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.extraction.keyphrase.extractors.ClassifierExtractor;
import ws.palladian.extraction.keyphrase.extractors.SimExtractor;
import ws.palladian.extraction.keyphrase.extractors.TfidfExtractor;
import ws.palladian.extraction.keyphrase.temp.Dataset2;
import ws.palladian.extraction.keyphrase.temp.DatasetHelper;
import ws.palladian.extraction.keyphrase.temp.DatasetItem;

public class KeyphraseExtractorEvaluator {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(KeyphraseExtractorEvaluator.class);

    /** The keyphrase extractors to evaluate. */
    private final List<KeyphraseExtractor> extractors;
    
    private final Dataset2 dataset;
    
    private final int numFolds;

    /** The stemmer is needed to compare the assigned tags to the existing ones. */
    private final SnowballStemmer stemmer;

    public KeyphraseExtractorEvaluator(Dataset2 dataset, int numFolds) {
        this.extractors = new ArrayList<KeyphraseExtractor>();
        this.dataset = dataset;
        this.numFolds = numFolds;
        this.stemmer = new englishStemmer();
    }

    public void addExtractor(KeyphraseExtractor extractor) {
        extractors.add(extractor);
    }
    
    public void evaluate() {
        LOGGER.info("dataset " + dataset);
        for (KeyphraseExtractor extractor : extractors) {
            evaluate(extractor);
        }
    }

    private void evaluate(KeyphraseExtractor extractor) {
        LOGGER.info("evaluating " + extractor.toString());
        Iterator<Dataset2[]> cvIterator = DatasetHelper.crossValidate(dataset, numFolds);
        int i = 1;
        KeyphraseExtractorEvaluationResult result = new KeyphraseExtractorEvaluationResult();
        while (cvIterator.hasNext()) {
            LOGGER.debug("fold " + i++ + "/" + numFolds);
            Dataset2[] trainTestSet = cvIterator.next();
            Dataset2 train = trainTestSet[0];
            Dataset2 test = trainTestSet[1];
            extractor.train(train);
            test(extractor, test, result);
        }
        LOGGER.info(result.toString());
        //result.printStatistics();
    }

    private void test(KeyphraseExtractor extractor, Dataset2 dataset, KeyphraseExtractorEvaluationResult result) {
        extractor.startExtraction();

        for (DatasetItem item : dataset) {

            // the manually assigned keyphrases
            Set<String> realKeyphrases = new HashSet<String>();
            String[] categoriesArray = item.getCategories();
            for (int i = 0; i < categoriesArray.length; i++) {
                realKeyphrases.add(categoriesArray[i].toLowerCase());
            }
            int realCount = realKeyphrases.size();
            Set<String> stemmedRealKeyphrases = stem(realKeyphrases);
            stemmedRealKeyphrases.addAll(realKeyphrases);

            String text;
            try {
                text = FileUtils.readFileToString(item.getFile());
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }

            List<Keyphrase> assignedKeyphrases = extractor.extract(text);
            int correctCount = 0;
            int assignedCount = assignedKeyphrases.size();

            // determine Pr/Rc values by considering assigned and real keyphrases
            for (Keyphrase assigned : assignedKeyphrases) {
                for (String real : stemmedRealKeyphrases) {

                    boolean correct = real.equalsIgnoreCase(assigned.getValue());
                    correct |= real.equalsIgnoreCase(assigned.getValue().replace(" ", ""));
                    correct |= real.equalsIgnoreCase(stem(assigned.getValue()));
                    correct |= real.equalsIgnoreCase(stem(assigned.getValue().replace(" ", "")));
                    if (correct) {
                        correctCount++;
                        break; // inner loop
                    }
                }
            }

            float precision = (float)correctCount / assignedCount;
            if (Float.isNaN(precision)) {
                precision = 0;
            }
            float recall = (float)correctCount / realCount;

            LOGGER.debug("real keyphrases: " + realKeyphrases);
            LOGGER.debug("assigned keyphrases: " + assignedKeyphrases);
            LOGGER.debug("real: " + realCount + " assigned: " + assignedCount + " correct: " + correctCount);
            LOGGER.debug("pr: " + precision + " rc: " + recall);
            LOGGER.debug("----------------------------------------------------------");

            result.addTestResult(precision, recall, assignedCount);
        }

        extractor.reset();
    }

    /** Stems each token of a phrase. */
    private String stem(String unstemmed) {
        StringBuilder result = new StringBuilder();
        // stem each part of the phrase
        String[] parts = unstemmed.toLowerCase().split(" ");
        for (String part : parts) {
            stemmer.setCurrent(part);
            stemmer.stem();
            result.append(stemmer.getCurrent());
        }
        return result.toString();
    }

    /** Stems a list of tokens. */
    private Set<String> stem(Set<String> unstemmed) {
        Set<String> result = new HashSet<String>();
        for (String unstemmedTag : unstemmed) {
            String stem = stem(unstemmedTag);
            result.add(stem);
        }
        return result;
    }

    public static void main(String[] args) {
        // KeyphraseExtractor keyphraseExtractor = new YahooTermExtraction();
        // KeyphraseExtractor keyphraseExtractor = new SimExtractor();
        // KeyphraseExtractor keyphraseExtractor = new MauiKeyphraseExtractor();
        // KeyphraseExtractor keyphraseExtractor = new TfidfExtractor();
//         Dataset2 dataset = DatasetHelper.loadDataset(new File("/Users/pk/Dropbox/Uni/Datasets/citeulike180/citeulike180index.txt"));
        Dataset2 dataset = DatasetHelper.loadDataset(new File("/Users/pk/Dropbox/Uni/Datasets/SemEval2010/semEvalTrainCombinedIndex.txt"));
        KeyphraseExtractorEvaluator evaluator = new KeyphraseExtractorEvaluator(dataset, 2);
        evaluator.addExtractor(new ClassifierExtractor());
//        evaluator.addExtractor(new TfidfExtractor());
//        evaluator.addExtractor(new SimExtractor());
        evaluator.evaluate();
    }

}
