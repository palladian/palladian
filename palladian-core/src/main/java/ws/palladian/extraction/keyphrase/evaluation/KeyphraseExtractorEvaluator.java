package ws.palladian.extraction.keyphrase.evaluation;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.extraction.keyphrase.extractors.MauiKeyphraseExtractor;
import ws.palladian.extraction.keyphrase.extractors.SimExtractor;
import ws.palladian.extraction.keyphrase.extractors.TfidfExtractor;
import ws.palladian.extraction.keyphrase.extractors.YahooTermExtraction;
import ws.palladian.extraction.keyphrase.temp.Dataset2;
import ws.palladian.extraction.keyphrase.temp.DatasetHelper;
import ws.palladian.extraction.keyphrase.temp.DatasetItem;

public class KeyphraseExtractorEvaluator {

    /** The logger for this class. */
    // private static final Logger LOGGER = Logger.getLogger(KeyphraseExtractorEvaluator.class);

    /** The stemmer is needed to compare the assigned tags to the existing ones. */
    private final SnowballStemmer stemmer = new englishStemmer();

    private final List<KeyphraseExtractor> extractors = new ArrayList<KeyphraseExtractor>();

    public void addExtractor(KeyphraseExtractor extractor) {
        extractors.add(extractor);
    }
    
    public void evaluate(KeyphraseExtractor extractor, Dataset2 dataset, int folds) {
        Iterator<Dataset2[]> cvIterator = DatasetHelper.crossValidate(dataset, folds);
        int i = 1;
        KeyphraseExtractorEvaluationResult result = new KeyphraseExtractorEvaluationResult();
        while (cvIterator.hasNext()) {
            System.out.println("fold " + i++ + "/" + folds);
            Dataset2[] trainTestSet = cvIterator.next();
            Dataset2 train = trainTestSet[0];
            Dataset2 test = trainTestSet[1];
            extractor.train(train);
            test(extractor, test, result);
        }
        result.printStatistics();
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

            System.out.println("real keyphrases: " + realKeyphrases);
            System.out.println("assigned keyphrases: " + assignedKeyphrases);
            System.out.println("real: " + realCount + " assigned: " + assignedCount + " correct: " + correctCount);
            System.out.println("pr: " + precision + " rc: " + recall);
            System.out.println("----------------------------------------------------------");

            result.addTestResult(precision, recall, assignedCount);
        }
        
        extractor.reset();

        //evaluationResult.printStatistics();
        //return evaluationResult;
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
        KeyphraseExtractor keyphraseExtractor = new TfidfExtractor();
        KeyphraseExtractorEvaluator evaluator = new KeyphraseExtractorEvaluator();
        evaluator.addExtractor(keyphraseExtractor);
        Dataset2 dataset = DatasetHelper.loadDataset(new File("/Users/pk/Desktop/temp/citeulike180index.txt"), "#");
        evaluator.evaluate(keyphraseExtractor, dataset, 2);
    }

}
