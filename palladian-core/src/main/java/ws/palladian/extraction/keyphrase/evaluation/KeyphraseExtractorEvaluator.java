package ws.palladian.extraction.keyphrase.evaluation;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import ws.palladian.extraction.keyphrase.Keyphrase;
import ws.palladian.extraction.keyphrase.KeyphraseExtractor;
import ws.palladian.extraction.keyphrase.extractors.MachineLearningBasedExtractor;
import ws.palladian.extraction.keyphrase.extractors.RuleBasedExtractor;
import ws.palladian.extraction.keyphrase.temp.Dataset2;
import ws.palladian.extraction.keyphrase.temp.DatasetHelper;
import ws.palladian.extraction.keyphrase.temp.DatasetItem;
import ws.palladian.helper.io.FileHelper;

public class KeyphraseExtractorEvaluator {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyphraseExtractorEvaluator.class);

    /** The keyphrase extractors to evaluate. */
    private final List<KeyphraseExtractor> extractors;

    /** The stemmer is needed to compare the assigned tags to the existing ones. */
    private final SnowballStemmer stemmer;

    public KeyphraseExtractorEvaluator() {
        this.extractors = new ArrayList<KeyphraseExtractor>();
        this.stemmer = new englishStemmer();
    }

    public void addExtractor(KeyphraseExtractor extractor) {
        extractors.add(extractor);
    }

    /**
     * <p>
     * Evaluate with one dataset using cross validation.
     * </p>
     * 
     * @param dataset The dataset used for cross validation.
     * @param numFolds The number of folds to perform.
     */
    public void evaluate(Dataset2 dataset, int numFolds) {
        LOGGER.info("dataset " + dataset);
        for (KeyphraseExtractor extractor : extractors) {
            evaluate(extractor, dataset, numFolds);
        }
    }

    /**
     * <p>
     * Evaluate with a train and a test dataset.
     * </p>
     * 
     * @param trainDataset The dataset used for training.
     * @param testDataset The dataset used for testing.
     */
    public void evaluate(Dataset2 trainDataset, Dataset2 testDataset) {
        for (KeyphraseExtractor extractor : extractors) {
            LOGGER.info("evaluating " + extractor.toString());
            KeyphraseExtractorEvaluationResult result = new KeyphraseExtractorEvaluationResult();
            extractor.train(trainDataset);
            test(extractor, testDataset, result);
            LOGGER.info(result.toString());
        }
    }

    private void evaluate(KeyphraseExtractor extractor, Dataset2 dataset, int numFolds) {
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
    }

    private void test(KeyphraseExtractor extractor, Dataset2 testDataset, KeyphraseExtractorEvaluationResult result) {
        extractor.startExtraction();

        int count = 0;
        for (DatasetItem item : testDataset) {

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
            //            try {
            text = FileHelper.tryReadFileToString(item.getFile());
            //                if (item.getFile().getName().endsWith(".html")) {
            //                    text = HtmlHelper.stripHtmlTags(text);
            //                    text = StringEscapeUtils.unescapeHtml(text);
            //                }
            //            } catch (IOException e) {
            //                throw new IllegalStateException(e);
            //            }

            List<Keyphrase> assignedKeyphrases = extractor.extract(text);

            // FIXME deduplicate
            Set<String> deduplicate = new HashSet<String>();
            Iterator<Keyphrase> iterator = assignedKeyphrases.iterator();
            while (iterator.hasNext()) {
                Keyphrase current = iterator.next();
                if (!deduplicate.add(current.getValue())) {
                    iterator.remove();
                }
            }

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

            LOGGER.debug("item " + count + "/" + testDataset.size());
            LOGGER.debug("real keyphrases: " + realKeyphrases);
            LOGGER.debug("assigned keyphrases: " + assignedKeyphrases);
            LOGGER.debug("real: " + realCount + " assigned: " + assignedCount + " correct: " + correctCount);
            LOGGER.debug("pr: " + precision + " rc: " + recall);
            LOGGER.debug("----------------------------------------------------------");

            result.addTestResult(precision, recall, assignedCount);
            count++;
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
        Dataset2 dataset = DatasetHelper.loadDataset(new File("/Users/pk/Dropbox/Uni/Datasets/citeulike180/citeulike180index.txt"));
        Dataset2 trainDataset = DatasetHelper.loadDataset(new File("/Users/pk/Dropbox/Uni/Datasets/SemEval2010/semEvalTrainCombinedIndex.txt"));
        Dataset2 testDataset = DatasetHelper.loadDataset(new File("/Users/pk/Dropbox/Uni/Datasets/SemEval2010/semEvalTestCombinedIndex.txt"));
        Dataset2 delicious1 = DatasetHelper.loadDataset(new File("/Users/pk/Desktop/delicioust140/split_aa.txt"));
        //        Dataset2 delicious1 = DatasetHelper.loadDataset(new File("/Users/pk/Desktop/delicioust140/big_train.txt"));
        Dataset2 delicious2 = DatasetHelper.loadDataset(new File("/Users/pk/Desktop/delicioust140/split_ab.txt"));
        //        Dataset2 deliciousComplete = DatasetHelper.loadDataset(new File("/Users/pk/Desktop/delicioust140_documents/delicioust140index_shuf.txt"));
        KeyphraseExtractorEvaluator evaluator = new KeyphraseExtractorEvaluator();
        //        evaluator.addExtractor(new TfidfExtractor());
        //        evaluator.addExtractor(new SimExtractor());
        //        evaluator.addExtractor(new MauiKeyphraseExtractor());
        evaluator.addExtractor(new RuleBasedExtractor());
        evaluator.addExtractor(new MachineLearningBasedExtractor());
        //        evaluator.evaluate(trainDataset, testDataset);
        evaluator.evaluate(delicious1, delicious2);
        //        evaluator.evaluate(deliciousComplete, 2);
    }

}
