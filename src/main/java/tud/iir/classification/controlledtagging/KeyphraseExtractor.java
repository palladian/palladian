package tud.iir.classification.controlledtagging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.tartarus.snowball.SnowballStemmer;

import tud.iir.classification.WordCorrelation;
import tud.iir.classification.controlledtagging.KeyphraseExtractorSettings.AssignmentMode;
import tud.iir.classification.controlledtagging.KeyphraseExtractorSettings.ReRankingMode;
import tud.iir.classification.page.evaluation.Dataset;
import tud.iir.extraction.keyphrase.AbstractKeyphraseExtractor;
import tud.iir.extraction.keyphrase.Keyphrase;
import tud.iir.helper.Counter;
import tud.iir.helper.FileHelper;
import tud.iir.helper.LineAction;
import tud.iir.helper.StopWatch;

/**
 * 
 * @author Philipp Katz
 * 
 */
public class KeyphraseExtractor extends AbstractKeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(KeyphraseExtractor.class);

    /** The TokenizerPlus is responsible for all tokenization steps. */
    private TokenizerPlus tokenizer = new TokenizerPlus();

    /** The corpus is a model for the whole document collection. */
    private Corpus corpus = new Corpus();

    /** The classifier is used for predicting relevance values for keyphrase candidates. */
    private CandidateClassifier classifier = new CandidateClassifier();

    /** This class encapsulates all the customizable settings. */
    private KeyphraseExtractorSettings settings = new KeyphraseExtractorSettings();

    public KeyphraseExtractor() {
        tokenizer.setUsePosTagging(false);
    }
    
    /**
     * Builds and saves the corpus.
     * 
     * @param dataset
     */
    public void buildCorpus(final Dataset dataset) {

        LOGGER.info("building corpus ...");

        StopWatch sw = new StopWatch();
        final Counter counter = new Counter();

        FileHelper.performActionOnEveryLine(dataset.getPath(), new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {

                String[] split = line.split(dataset.getSeparationString());
                if (split.length < 2) {
                    return;
                }

                String text;
                if (dataset.isFirstFieldLink()) {
                    text = FileHelper.readFileToString(dataset.getRootPath() + split[0]);
                } else {
                    text = split[0];
                }
                
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
     * Builds and saves the corpus.
     * 
     * @param dataset
     */
    public void buildCorpus(String filePath) {
        Dataset dataset = new Dataset();
        dataset.setFirstFieldLink(false);
        dataset.setSeparationString("#");
        buildCorpus(dataset);
    }
    
    /**
     * Builds the classifier.
     * 
     * @param filePath
     * @param limit
     */
    public void buildClassifier(final Dataset dataset, final int limit){

        // write the training data to CSV file
        final String trainDataPath = "data/temp/KeyphraseExtractorTraining.csv";
        createTrainData(dataset, limit, trainDataPath);

        // train and save the classifier
        StopWatch sw = new StopWatch();
        LOGGER.info("training classifier ...");        

        // TODO save memory; this is necessary, as the corpus consumes great amounts of memory, but
        // fortunately we don't need the corpus for the training process
        corpus = null;

        // train a new Classifier using the CSV data from above
        classifier = new CandidateClassifier();
        classifier.trainClassifier(trainDataPath, true);

        // save the trained classifier
        saveClassifier();

        LOGGER.info("finished training in " + sw.getElapsedTimeString());
    }
    
    /**
     * Builds the classifier.
     * 
     * @param dataset
     */
    public void buildClassifier(Dataset dataset) {
        buildClassifier(dataset, -1);
    }

    /**
     * Builds the classifier.
     * 
     * @param filePath
     * @param limit
     */
    public void buildClassifier(String filePath, final int limit) {
        Dataset dataset = new Dataset();
        dataset.setFirstFieldLink(false);
        dataset.setSeparationString("#");
        buildClassifier(dataset, limit);
    }
    
    /**
     * Create CSV data for training the classifier. This data can either be used directly for Weka or as imported to
     * KNIME.
     * 
     * @param dataset
     * @param limit
     * @param trainDataPath
     */
    public void createTrainData(final Dataset dataset, final int limit, String trainDataPath) {
        LOGGER.info("creating training data for classifier ...");

        StopWatch sw = new StopWatch();
        final Counter counter = new Counter();

        // keep the CSV training data in memory for now
        final StringBuilder trainData = new StringBuilder();

        // create the training data for the classifier
        FileHelper.performActionOnEveryLine(dataset.getPath(), new LineAction() {

            @Override
            public void performAction(String line, int lineNumber) {
                String[] split = line.split(dataset.getSeparationString());

                if (split.length < 2) {
                    return;
                }

                String text;
                if (dataset.isFirstFieldLink()) {
                    text = FileHelper.readFileToString(dataset.getRootPath() + split[0]);
                } else {
                    text = split[0];
                }
                // create the document model
                DocumentModel candidates = createDocumentModel(text);

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
                    isCandidate = isCandidate || tags.contains(candidate.getStemmedValue().replace(" ", ""));
                    isCandidate = isCandidate || tags.contains(candidate.getValue());
                    isCandidate = isCandidate || tags.contains(candidate.getValue().replace(" ", ""));
                    candidate.setPositive(isCandidate);
                }

                // if this is the first iteration, write header with feature names;
                // this is only for convenience reasons, for example if we want to
                // experiment with the classification with KNIME
                if (counter.getCount() == 0) {
                    Set<String> featureNames = candidates.iterator().next().getFeatures().keySet();
                    // trainData.append("#");
                    trainData.append(StringUtils.join(featureNames, ";")).append("\n");
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
        FileHelper.writeToFile(trainDataPath, trainData);
        LOGGER.info("created training data in " + sw.getElapsedTimeString());
    }

    /**
     * Add the supplied text to the corpus. This is used for the TF-IDF calculation.
     * 
     * @param text
     */
    public void addToCorpus(String text) {
        List<Token> tokens = tokenize(text);
        corpus.addPhrases(tokens);
    }

    /**
     * Add the supplied text and the keyphrases to the corpus. The text is used for the TF-IDF calculation, the supplied
     * keyphrases for building the dictionary with the prior probabilities and the WordCorrelationMatrix.
     * 
     * @param text
     * @param keyphrases
     */
    public void addToCorpus(String text, Set<String> keyphrases) {

        // tokenize the text and add the tokens/phrases to the corpus
        List<Token> tokens = tokenize(text);
        corpus.addPhrases(tokens);

        // the corpus stores the *stemmed* keyphrases
        keyphrases = stem(keyphrases);
        corpus.addKeyphrases(keyphrases);

    }

    public void saveCorpus() {
        String filePath = settings.getModelPath() + "/corpus.ser";
        LOGGER.info("saving corpus to " + filePath + " ...");
        StopWatch sw = new StopWatch();
        corpus.makeRelativeScores();
        FileHelper.serialize(corpus, filePath);
        LOGGER.info("saved corpus in " + sw.getElapsedTimeString());
    }
    
    public void loadCorpus() {
        LOGGER.info("loading corpus ...");
        StopWatch sw = new StopWatch();
        corpus = FileHelper.deserialize(settings.getModelPath() + "/corpus.ser");
        LOGGER.info("loaded corpus in " + sw.getElapsedTimeString());
    }
    
    public void saveClassifier() {
        String filePath = settings.getModelPath() + "/classifier.ser";
        LOGGER.info("saving classifier " + filePath + " ...");
        StopWatch sw = new StopWatch();
        classifier.saveTrainedClassifier(filePath);
        LOGGER.info("saved classifier in " + sw.getElapsedTimeString());
    }

    public void loadClassifier() {
        LOGGER.info("loading classifier ...");
        StopWatch sw = new StopWatch();
        classifier.loadTrainedClassifier(settings.getModelPath() + "/classifier.ser");
        LOGGER.info("loaded classifier in " + sw.getElapsedTimeString());
    }

    @Override
    public Set<Keyphrase> extract(String text) {

        DocumentModel candidates = createDocumentModel(text);

        // eliminate undesired candidates in advance
        ListIterator<Candidate> listIterator = candidates.listIterator();
        while (listIterator.hasNext()) {
            Candidate candidate = listIterator.next();

            boolean ignore = settings.getStopwords().contains(candidate.getValue());
            ignore = ignore || !settings.getPattern().matcher(candidate.getValue()).matches();
            ignore = ignore || (settings.isControlledMode() && candidate.getPrior() == 0);
            ignore = ignore || candidate.getCount() < settings.getMinOccurenceCount();

            if (ignore) {
                listIterator.remove();
            }
        }

        // perform the regression for ranking the candidates
        classifier.classify(candidates);

        // Collections.sort(candidates, new CandidateComparator());
        // for (Candidate candidate : candidates) {
        // LOGGER.debug(candidate.getRegressionValue() + "\t" + candidate);
        // }

        // do the correlation based re-ranking
        reRankCandidates(candidates);

        // create the final result, take the top n candidates
        limitResult(candidates);

        // return candidates;
        Set<Keyphrase> keyphrases = new HashSet<Keyphrase>();
        for (Candidate candidate : candidates) {
            keyphrases.add(new Keyphrase(candidate.getValue(), candidate.getRegressionValue()));
        }
        
        return keyphrases;

    }

    /**
     * Reduces the DocumentModel (e. g. the list of {@link Candidate}s) using the specified {@link AssignmentMode}.
     * 
     * @param candidates
     */
    private void limitResult(DocumentModel candidates) {

        // sort the candidates by regression value
        Collections.sort(candidates, new CandidateComparator());

        // ListIterator for manipulating the list
        ListIterator<Candidate> listIterator = candidates.listIterator();

        switch (settings.getAssignmentMode()) {

            // assign a maximum number of keyphrases as result
            case FIXED_COUNT:
                if (candidates.size() > settings.getKeyphraseCount()) {
                    candidates.subList(settings.getKeyphraseCount(), candidates.size()).clear();
                }
                break;

            // assign all keyphrases which have a weight above the specified threshold
            case THRESHOLD:
                while (listIterator.hasNext()) {
                    if (listIterator.next().getRegressionValue() <= settings.getKeyphraseThreshold()) {
                        listIterator.remove();
                    }
                }
                break;

            // first assign a maximum number of keyphrases (FIXED_COUNT), but if there are more keyphrases
            // with weights above the specified threshold, we assign more than the specified count
            case COMBINED:
                while (listIterator.hasNext()) {
                    Candidate next = listIterator.next();
                    boolean moreThanLimit = listIterator.nextIndex() > settings.getKeyphraseCount();
                    boolean lessThanThreshold = next.getRegressionValue() <= settings.getKeyphraseThreshold();
                    if (moreThanLimit && lessThanThreshold) {
                        listIterator.remove();
                    }
                }
                break;
        }

    }

    /**
     * Performs a correlation based re-ranking of the supplied {@link Candidate}s. The re-ranking strategies are
     * explained in Diploma thesis <i>NewsSeecr -- Clustering und Ranking von Nachrichten zu Named Entities aus
     * Newsfeeds</i>, Philipp Katz, 2010. The re-ranking should basically be obsolete now by using the classification
     * based approach.
     * 
     * The re-ranking factor can be adjusted using {@link KeyphraseExtractorSettings#setCorrelationWeight(float)}. This
     * number is quite ad-hoc and has to be determined experimentally.
     * 
     * @param candidates
     */
    private void reRankCandidates(DocumentModel candidates) {

        StopWatch sw = new StopWatch();
        
        if (candidates.isEmpty()) {
            return;
        }
        
        Collections.sort(candidates, new CandidateComparator());

        // experimental: to normalize the range of the re-ranked tags back to their original range,
        // by keeping the lower/upper bounds, so we keep the general properties of the TF/IDF -- elsewise
        // we will get outliers which are considerably bigger than most of the other tag weights.
        double oldMin = candidates.get(0).getRegressionValue();
        double oldMax = candidates.get(candidates.size() - 1).getRegressionValue();

        // Option 1: do a "shallow" re-ranking, only considering top-tag (n)
        if (settings.getReRankingMode() == ReRankingMode.SHALLOW_CORRELATION_RERANKING) {
            Iterator<Candidate> candidateIterator = candidates.iterator();
            Candidate topCandidate = candidateIterator.next();

            while (candidateIterator.hasNext()) {
                Candidate currentCandidate = candidateIterator.next();

                WordCorrelation correlation = corpus.getCorrelation(topCandidate, currentCandidate);
                if (correlation != null) {
                    currentCandidate.increaseRegressionValue(settings.getCorrelationWeight() * correlation.getRelativeCorrelation());
                }
            }
        }

        // Option 2: do a "deep" re-ranking, considering correlations between each possible combination
        else if (settings.getReRankingMode() == ReRankingMode.DEEP_CORRELATION_RERANKING) {
            Candidate[] candidatesArray = candidates.toArray(new Candidate[candidates.size()]);

            // experimental:
            // normalization factor; we have (n - 1) + (n - 2) + ... + 1 = n * (n - 1) / 2 re-rankings.
            int numReRanking = candidatesArray.length * (candidatesArray.length - 1) / 2;
            // FIX-ME why dont we put the numReRanking division outside the loop?
            float factor = (float) settings.getCorrelationWeight() / numReRanking;

            for (int i = 0; i < candidatesArray.length; i++) {
                Candidate candidate1 = candidatesArray[i];
                
                for (int j = i; j < candidatesArray.length; j++) {
                    Candidate candidate2 = candidatesArray[j];
                    
                    WordCorrelation correlation = corpus.getCorrelation(candidate1, candidate2);
                    if (correlation != null) {
                        float reRanking = (float) (factor * correlation.getRelativeCorrelation());

                        assert !Double.isInfinite(reRanking);
                        assert !Double.isNaN(reRanking);

                        candidate1.increaseRegressionValue(reRanking);
                        candidate2.increaseRegressionValue(reRanking);
                    }
                }
            }
        }

        // re-sort the list, as ranking weights have changed
        Collections.sort(candidates, new CandidateComparator());

        // do the scaling back to the original range (see comment above)
        double newMin = candidates.get(0).getRegressionValue();
        double newMax = candidates.get(candidates.size() - 1).getRegressionValue();

        if (newMin != newMax) { // avoid division by zero
            for (Candidate candidate : candidates) {

                // http://de.wikipedia.org/wiki/Normalisierung_(Mathematik)
                double current = candidate.getRegressionValue();
                double normalized = (current - newMin) * ((oldMax - oldMin) / (newMax - newMin)) + oldMin;
                candidate.setRegressionValue(normalized);

            }
        }

        LOGGER.trace("correlation reranking for " + candidates.size() + " in " + sw.getElapsedTimeString());

    }

    /**
     * Creates a {@link DocumentModel} from the specified text. The DocumentModel contains all potential keyphrase
     * {@link Candidate}s. The creation of the DocumentModel consists of the following steps:
     * 
     * <ol>
     * <li>Tokenization of the supplied text to create {@link Token}s,</li>
     * <li>Creation of a new DocumentModel which represents the text and consists of the Tokens,</li>
     * <li>Consolidation of the single Tokens to Candidates; this means all Candidates which have identical stemmed
     * values are merged together.</li>
     * </ol>
     * 
     * @param text
     * @return
     */
    private DocumentModel createDocumentModel(String text) {

        DocumentModel model = new DocumentModel(corpus);
        List<Token> tokens = tokenize(text);
        model.addTokens(tokens);
        model.createCandidates();

        // when we are in controlled mode, we remove all non-keyphrases from the list of candidates.
        if (settings.isControlledMode()) {
            model.removeNonKeyphrases();
        }

        return model;

    }

    private List<Token> tokenize(String text) {

        List<Token> tokens = new ArrayList<Token>();
        List<Token> uniGrams = tokenizer.tokenize(text);
        List<Token> collocations = tokenizer.makeCollocations(uniGrams, settings.getPhraseLength());

        tokens.addAll(uniGrams);
        tokens.addAll(collocations);

        return tokens;

    }

    /**
     * Stem a term or a phrase using the specified stemmer. For phrases (e. g. multiple terms separated by space
     * characters) each single term is stemmed, and each stem is concatenated together.
     * 
     * @param unstemmed
     * @return
     */
    /* package */ String stem(String unstemmed) {
        
        StringBuilder sb = new StringBuilder();        
        SnowballStemmer stemmer = settings.getStemmer();
        
        // stem each part of the phrase
        String[] parts = unstemmed.toLowerCase().split(" ");
        for (String part : parts) {
            stemmer.setCurrent(part);
            stemmer.stem();
            sb.append(stemmer.getCurrent());            
        }
        
        return sb.toString();
        
    }

    /**
     * Stem a set of terms/phrases (see {@link #stem(String)}.
     * 
     * @param unstemmed
     * @return
     */
    /* package */ Set<String> stem(Set<String> unstemmed) {
        Set<String> result = new HashSet<String>();
        for (String unstemmedTag : unstemmed) {
            String stem = stem(unstemmedTag);
            result.add(stem);
        }
        return result;
    }
    
    /**
     * Get access to the settings.
     * 
     * @return
     */
    public KeyphraseExtractorSettings getSettings() {
        return settings;
    }
    
    /**
     * Set a specific {@link KeyphraseExtractorSettings} instance as settings.
     * 
     * @param settings
     */
    public void setSettings(KeyphraseExtractorSettings settings) {
        this.settings = settings;
    }

    public static void main(String[] args) {

        final KeyphraseExtractor extractor = new KeyphraseExtractor();
        
        KeyphraseExtractorSettings extractorSettings = extractor.getSettings();
        extractorSettings.setAssignmentMode(AssignmentMode.COMBINED);
        extractorSettings.setReRankingMode(ReRankingMode.NO_RERANKING);
        extractorSettings.setMinOccurenceCount(1);        
        extractorSettings.setKeyphraseCount(10);
        extractorSettings.setKeyphraseThreshold(0.3f);
        

        
        
        
        
//         String filePath = "data/tagData_shuf_10000aa";
         String filePath = "/Users/pk/Dropbox/tmp/tagData_shuf_10000aa";
//        String filePath = "fao_splitaa";
////        String classPath = "classifier_fao.ser";
         String classPath = "bagging_classifier.ser";
         
         // String classPath = "neuralnet_classifier.ser";

        extractorSettings.setModelPath("data/corpus_model.ser");
//        extractorSettings.setAssignmentMode(AssignmentMode.COMBINED);
//        extractorSettings.setReRankingMode(ReRankingMode.DEEP_CORRELATION_RERANKING);
//        extractorSettings.setCorrelationWeight(50000);
// XXX        extractorSettings.setControlledMode(false);
        extractorSettings.setControlledMode(true);
        
        // //////////////////////////////////////////////
        // CORPUS CREATION
        // //////////////////////////////////////////////
        extractor.buildCorpus(filePath);
//        extractor.loadCorpus();

        // //////////////////////////////////////////////
        // FEATURE SET FOR TRAINING CREATION
        // //////////////////////////////////////////////
        extractor.buildClassifier(filePath, 750);
//        extractor.saveClassifier(classPath);
//        System.exit(0);

        // //////////////////////////////////////////////
        // EVALUATION
        // //////////////////////////////////////////////

//        extractor.loadClassifier(classPath);
        extractor.loadCorpus();
//        extractor.evaluate("fao_splitab", 500);
//        extractor.evaluate("/Users/pk/Dropbox/tmp/tagData_shuf_10000ab", 1000);
        
        
//        extractor.getSettings().setReRankingMode(ReRankingMode.NO_RERANKING);
//       extractor.evaluate("fao_splitab", 100);
        
               // String stem = extractor.stem("the quick brown foxes jumps over the lazy dogs.");
               // System.out.println(stem);
               // System.exit(0);
//        Crawler c = new Crawler();
//        String result = c.download("http://www.i-funbox.com/");
//        result = HTMLHelper.htmlToString(result, true);
//        extractor.getSettings().setKeyphraseCount(20);
//        List<Candidate> extract = extractor.extract(result);
//        System.out.println(extract);

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


}
