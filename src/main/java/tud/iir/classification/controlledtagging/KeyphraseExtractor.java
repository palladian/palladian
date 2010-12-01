package tud.iir.classification.controlledtagging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.math.stat.descriptive.DescriptiveStatistics;
import org.apache.log4j.Logger;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import tud.iir.classification.Stopwords;
import tud.iir.classification.WordCorrelation;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetCallback;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetEntry;
import tud.iir.classification.controlledtagging.DeliciousDatasetReader.DatasetFilter;
import tud.iir.helper.Counter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.StopWatch;

/**
 * 
 * optimum vlaues:
 * 
 * avgPr: 0.4863928631767631
 * avgRc: 0.23635926371440288
 * avgF1: 0.3181269338103561
 * 
 * @author Philipp Katz
 * 
 */
public class KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(KeyphraseExtractor.class);

    private SnowballStemmer stemmer = new englishStemmer();
    private Stopwords stopwords = new Stopwords(Stopwords.Predefined.EN);
    private TokenizerPlus tokenizer = new TokenizerPlus();

    private Corpus corpus = new Corpus();
    private CandidateClassifier classifier = new CandidateClassifier();

    private boolean controlledMode = false; // XXX testing

    private String modelName = "controlledTagger";

    public KeyphraseExtractor() {
        tokenizer.setUsePosTagging(false);
    }

    public void buildCorpus(String filePath) {
        
        LOGGER.info("building corpus ...");

        StopWatch sw = new StopWatch();
        final Counter counter = new Counter();
        
        FileHelper.performActionOnEveryLine(filePath, new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] split = line.split("#");
                if (split.length < 2) {
                    return;
                }

                String text = split[0];
                Set<String> tags = new HashSet<String>();
                for (int i = 1; i < split.length; i++) {
                    tags.add(split[i]);
                }
                addToCorpus(text, tags);

                counter.increment();
                if (counter.getCount() % 10 == 0) {
                    LOGGER.info("added " + counter + " lines");
                }
            }
        });
        
        saveCorpus();
        LOGGER.info("built and saved corpus in " + sw.getElapsedTimeString());

    }

    /**
     * TODO We can keep the training data in memory. And add an option for exporting CSV for KNIME.
     * 
     * @param filePath
     * @param limit
     */
    public void buildClassifier(String filePath, final int limit) {

        LOGGER.info("creating training data for classifier ...");

        StopWatch sw = new StopWatch();
        final Counter counter = new Counter();

        // keep the CSV training data in memory for now
        final StringBuilder trainData = new StringBuilder();

        // create the training data for the classifier
        FileHelper.performActionOnEveryLine(filePath, new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split("#");

                if (split.length < 2) {
                    return;
                }

                // create the document model
                DocumentModel candidates = createDocumentModel(split[0]);

                // the manually assigned keyphrases
                Set<String> tags = new HashSet<String>();
                for (int i = 1; i < split.length; i++) {
                    tags.add(split[i].toLowerCase());
                }

                // keep stemmed and unstemmed representation
                Set<String> stemmedTags = stem(tags);
                tags.addAll(stemmedTags);

                // mark positive candidates, i.e. those which were manually assigned
                // in the training data
                for (Candidate candidate : candidates) {
                    boolean isCandidate = tags.contains(candidate.getStemmedValue());
                    isCandidate = isCandidate || tags.contains(candidate.getValue());
                    isCandidate = isCandidate || tags.contains(candidate.getValue().replace(" ", ""));
                    candidate.setPositive(isCandidate);
                }

                // if this is the first iteration, write header with feature names;
                // this is only for convenience reasons, for example if we want to
                // experiment with the classification with KNIME
                if (counter.getCount() == 0) {
                    Set<String> featureNames = candidates.iterator().next().getFeatures().keySet();
                    trainData.append("#").append(StringUtils.join(featureNames, ";")).append("\n");
                }

                trainData.append(candidates.toCSV());

                counter.increment();
                if (counter.getCount() % 10 == 0) {
                    LOGGER.info("added " + counter + " lines");
                }
                if (counter.getCount() == limit) {
                    breakLineLoop();
                }
            }
        });

        // write the train data for the classifier to CSV file
        FileHelper.writeToFile("data/temp/KeyphraseExtractorTraining.csv", trainData);
        LOGGER.info("created training data in " + sw.getElapsedTimeString());
        
        // train and save the classifier
        LOGGER.info("training classifier ...");
        sw.start();
        
        // save memory; this is necessary, as the corpus consumes great amounts of memory, but
        // fortunately we don't need the corpus for the training process
        corpus = null;
        
        classifier = new CandidateClassifier();
        classifier.trainClassifier("data/temp/KeyphraseExtractorTraining.csv");
        classifier.saveTrainedClassifier();

        LOGGER.info("finished training in " + sw.getElapsedTimeString());

    }

    public void addToCorpus(String text) {

        List<Token> tokens = tokenize(text);
        corpus.addPhrases(tokens);

    }

    public void addToCorpus(String text, Set<String> keyphrases) {

        // tokenize the text and add the tokens/phrases to the corpus
        List<Token> tokens = tokenize(text);
        corpus.addPhrases(tokens);

        // the corpus stores the stemmed keyphrases!
        keyphrases = stem(keyphrases);
        corpus.addKeyphrases(keyphrases);

    }

    public void saveCorpus() {
        corpus.makeRelativeScores();
        FileHelper.serialize(corpus, modelName + ".ser");
    }

    public void loadCorpus() {
        LOGGER.info("loading corpus ...");
        StopWatch sw = new StopWatch();
        corpus = FileHelper.deserialize(modelName + ".ser");
        LOGGER.info("loaded corpus in " + sw.getElapsedTimeString());
    }

    public void loadClassifier() {
        LOGGER.info("loading classifier ...");
        StopWatch sw = new StopWatch();
        classifier.useTrainedClassifier();
        LOGGER.info("loaded classifier in " + sw.getElapsedTimeString());
    }
    
    public List<Candidate> extract(String text) {
        
        DocumentModel candidates = createDocumentModel(text);
        
        // eliminate undesired candidates in advance
        ListIterator<Candidate> listIterator = candidates.listIterator();
        while (listIterator.hasNext()) {
            Candidate candidate = listIterator.next();
            
            boolean ignore = stopwords.contains(candidate.getValue());
            ignore = ignore || !candidate.getValue().matches("[a-zA-Z\\s]{3,}");
            ignore = ignore || (controlledMode && candidate.getPrior() == 0);
            
            if (ignore) {
                listIterator.remove();
            }
        }
        
        // perform the regression for ranking the candidates
        classifier.classify(candidates);
        
        // do the correlation based re-ranking
        // TODO refactor this to its own method ////////////////////////////////////
        Candidate[] candidateArray = candidates.toArray(new Candidate[0]);
        int numReRanking = candidateArray.length * (candidateArray.length - 1) / 2;

        // TODO parameter
        float correlationWeight = 90000;

        for (int i = 0; i < candidateArray.length; i++) {
            Candidate cand1 = candidateArray[i];
            for (int j = i; j < candidateArray.length; j++) {
                Candidate cand2 = candidateArray[j];
                
                WordCorrelation correlation = corpus.getCorrelation(cand1, cand2);
                if (correlation != null) {
                    float reRanking = (float) ((correlationWeight / numReRanking) * correlation
                            .getRelativeCorrelation());
                    cand1.setRegressionValue(cand1.getRegressionValue() + reRanking);
                    cand2.setRegressionValue(cand2.getRegressionValue() + reRanking);
                }
            }
        }
        /////////////////////////////////////////////////////////////////////////////
        
        // sort the candidates by regression value
        Collections.sort(candidates, new CandidateComparator());
        
        // create the final result, take the top n candidates
        if (candidates.size() > 10) {
            candidates.subList(10, candidates.size()).clear();
        }
        
        return candidates;
        
    }

    public float[] evaluate(String text, Set<String> tags) {

        Set<String> stemmedTags = stem(tags);

        
        List<Candidate> candidatesList = extract(text);
        
        
        int realCount = stemmedTags.size();

        stemmedTags.addAll(tags); // XXX

        int correctlyAssigned = 0;
        for (Candidate candidate : candidatesList) {
            for (String realTag : stemmedTags) {

                boolean isCorrectlyAssigned = false;
                isCorrectlyAssigned = isCorrectlyAssigned || realTag.equalsIgnoreCase(candidate.getStemmedValue());
                isCorrectlyAssigned = isCorrectlyAssigned || realTag.equalsIgnoreCase(candidate.getValue());
                isCorrectlyAssigned = isCorrectlyAssigned
                        || realTag.equalsIgnoreCase(candidate.getValue().replace(" ", ""));

                if (isCorrectlyAssigned) {
                    correctlyAssigned++;
                    break; // XXX
                }

            }
            System.out.println(" " + candidate.getValue());
        }

        int totalAssigned = candidatesList.size();

        float precision = (float) correctlyAssigned / totalAssigned;
        if (Float.isNaN(precision)) {
            precision = 0;
        }
        float recall = (float) correctlyAssigned / realCount;

        // System.out.println("real: " + stemmedTags);
        // System.out.println("assigned: " + candidatesList);
        System.out.println("correctlyAssigned:" + correctlyAssigned);
        System.out.println("totalAssigned:" + totalAssigned);
        System.out.println("realCount: " + realCount);
        System.out.println("pr: " + precision);
        System.out.println("rc: " + recall);
        System.out.println("------------------");

        float[] result = new float[2];
        result[0] = precision;
        result[1] = recall;
        return result;

    }

    private DocumentModel createDocumentModel(String text) {

        DocumentModel model = new DocumentModel(corpus);
        List<Token> tokens = tokenize(text);
        model.addTokens(tokens);
        model.createCandidates();

        return model;

    }

    public List<Token> tokenize(String text) {

        List<Token> tokens = new ArrayList<Token>();
        List<Token> uniGrams = tokenizer.tokenize(text);
        List<Token> collocations = tokenizer.makeCollocations(uniGrams, 5);

        tokens.addAll(uniGrams);
        tokens.addAll(collocations);

        return tokens;

    }

    public String stem(String unstemmed) {
        stemmer.setCurrent(unstemmed.toLowerCase());
        stemmer.stem();
        return stemmer.getCurrent();
    }

    public Set<String> stem(Set<String> unstemmed) {
        Set<String> result = new HashSet<String>();
        for (String unstemmedTag : unstemmed) {
            String stem = stem(unstemmedTag);
            result.add(stem);
        }
        return result;
    }

    public static void main(String[] args) {

        final KeyphraseExtractor extractor = new KeyphraseExtractor();

        // String text3 =
        // "Beijing Duck is mostly prized for the thin, crispy duck skin with authentic versions of the dish serving mostly the skin. Beijing Duck is delicious. Beijing Duck is expensive.";
        //
        // DocumentModel cnd2 = extractor.makeCandidates(text3);
        // System.out.println(cnd2);
        //
        // System.exit(0);

        // Crawler crawler = new Crawler();
        //
        // Document doc = crawler.getWebDocument("http://en.wikipedia.org/wiki/The_Garden_of_Earthly_Delights");
        // String text = HTMLHelper.htmlToString(doc);
        //
        // DocumentModel cnd = extractor.makeCandidates(text);
        // cnd.cleanCandidates(5); // remove candidates which occur less than 5
        // System.out.println(cnd);
        //
        // System.exit(0);

        // //////////////////////////////////////////////
        // CORPUS CREATION
        // //////////////////////////////////////////////
        // createCorpus(extractor);
////        String filePath = "data/tag_dataset_10000.txt";
////        extractor.buildCorpus(filePath);

        // //////////////////////////////////////////////
        // FEATURE SET FOR TRAINING CREATION
        // //////////////////////////////////////////////
        // createTrainData(extractor);
////        extractor.buildClassifier(filePath, 1000);

        // //////////////////////////////////////////////
        // EVALUATION
        // //////////////////////////////////////////////
        evaluate(extractor);

        System.exit(0);

        String d1 = "If it walks like a duck and quacks like a duck, it must be a duck.";
        String d2 = "Beijing Duck is mostly prized for the thin, crispy duck skin with authentic versions of the dish serving mostly the skin.";
        String d3 = "Bugs' ascension to stardom also prompted the Warner animators to recast Daffy Duck as the rabbit's rival, intensely jealous and determined to steal back the spotlight while Bugs remained indifferent to the duck's jealousy, or used it to his advantage. This turned out to be the recipe for the success of the duo.";
        String d4 = "6:25 PM 1/7/2007 blog entry: I found this great recipe for Rabbit Braised in Wine on cookingforengineers.com.";
        // String d5 =
        // "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipies for Jiaozi.";
        String d5 = "Last week Li has shown you how to make the Sechuan duck. Today we'll be making Chinese dumplings (Jiaozi), a popular dish that I had a chance to try last summer in Beijing. There are many recipe for Jiaozi.";

        extractor.addToCorpus(d1);
        extractor.addToCorpus(d2);
        extractor.addToCorpus(d3);
        extractor.addToCorpus(d4);
        extractor.addToCorpus(d5);

        // System.out.println(". -> " + extractor.corpus.getInverseDocumentFrequency("."));

        DocumentModel candidates = extractor.createDocumentModel(d2); // (, 1);
        System.out.println(candidates);
        System.exit(0);

        String text2 = "the quick brown fox jumps over the lazy dog. the quick brown fox. brownfox. brownfox. brownfox. brownfox. brownfox.";
        // String text = "apple apple apples apples";
        // String text = "Apple sells phones called iPhones. The iPhone is a smart phone. Smart phones are great!";
        // String text = "iPhones iPhone iPhones";

        DocumentModel makeCandidates = extractor.createDocumentModel(text2); // , 1);
        // System.out.println(makeCandidates);
        System.out.println(makeCandidates.toCSV());
        System.exit(0);

        // List<Token> tokens = extractor.tokenize(text, -1);
        // System.out.println(tokens);
        // DocumentModel model = extractor.tokenize(text, 2);
        // System.out.println(model);
        // List<Token> tokenize2 = extractor.tokenize2(text, 3);
        // CollectionHelper.print(tokenize2);
        // DocumentModel c = extractor.makeCandidates(text, 3);
        // System.out.println(c);
        //
        // System.exit(0);

        // String x = FileHelper.readFileToString("tokenizerProblem.txt");
        // List<String> t = Tokenizer.tokenize(x);
        // System.out.println(t.size());
        //
        // DocumentModel tokenize = extractor.tokenize(x, 3);
        // Collection<Candidate> candidates = tokenize.getCandidates(2);
        // for (Candidate candidate : candidates) {
        // System.out.println(candidate);
        // }
        // //System.out.println(tokenize);
        // System.exit(0);

        // StopWatch sw = new StopWatch();
        // extractor.extractFromFile("dataset_10000.txt");
        // System.out.println(sw.getElapsedTimeString());

        System.exit(1);
        //
        // String text =
        // "the quick brown fox jumps over the lazy dog. the quick brown fox. brownfox. brownfox. brownfox. brownfox. brownfox.";
        // // List<Token> tokens = extractor.tokenize(text, -1);
        // // System.out.println(tokens);
        // DocumentModel model = extractor.extract(text, 2);
        // System.out.println(model);

    }

    @SuppressWarnings("unused")
    private static void evaluate(final KeyphraseExtractor extractor) {
        
        extractor.loadCorpus();
        extractor.loadClassifier();
        
        final DescriptiveStatistics prStats = new DescriptiveStatistics();
        final DescriptiveStatistics rcStats = new DescriptiveStatistics();
        final Counter counter = new Counter();

        DeliciousDatasetReader reader = new DeliciousDatasetReader();

        DatasetFilter filter = new DatasetFilter();
        filter.addAllowedFiletype("html");
        filter.setMinUsers(50);
        filter.setMaxFileSize(600000);
        reader.setFilter(filter);

        DatasetCallback callback = new DatasetCallback() {

            @Override
            public void callback(DatasetEntry entry) {

                String content = FileHelper.readFileToString(entry.getPath());
                content = HTMLHelper.htmlToString(content, true);

                float[] prRc = extractor.evaluate(content, entry.getTags().uniqueSet());
                System.out.println("pr:" + prRc[0] + " rc:" + prRc[1]);
                counter.increment();

                prStats.addValue(prRc[0]);
                rcStats.addValue(prRc[1]);

            }
        };
        reader.read(callback, 1000);

        double meanPr = prStats.getMean();
        double meanRc = rcStats.getMean();
        double meanF1 = 2 * meanPr * meanRc / (meanPr + meanRc);

        System.out.println("avgPr: " + meanPr);
        System.out.println("avgRc: " + meanRc);
        System.out.println("avgF1: " + meanF1);

    }


}
