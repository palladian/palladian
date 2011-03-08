package ws.palladian.extraction.keyphrase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.tartarus.snowball.SnowballStemmer;

import ws.palladian.classification.FeatureObject;
import ws.palladian.classification.WekaClassifierWrapper;
import ws.palladian.classification.WordCorrelation;
import ws.palladian.extraction.keyphrase.KeyphraseExtractorSettings.AssignmentMode;
import ws.palladian.extraction.keyphrase.KeyphraseExtractorSettings.ReRankingMode;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;

/**
 * 
 * @author Philipp Katz
 * 
 */
public class PalladianKeyphraseExtractor extends KeyphraseExtractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(PalladianKeyphraseExtractor.class);

    /** Limit for the amount of train data for the classifier; necessary because of memory limits. */
    private static final int TRAIN_DATA_LIMIT = 160000;

    /** The TokenizerPlus is responsible for all tokenization steps. */
    private TokenizerPlus tokenizer;

    /** The corpus is a model for the whole document collection. */
    private Corpus corpus = new Corpus();

    /** The classifier is used for predicting relevance values for keyphrase candidates. */
    private WekaClassifierWrapper classifier = new WekaClassifierWrapper(WekaClassifierWrapper.BAGGING);

    /** This class encapsulates all the customizable settings. */
    private KeyphraseExtractorSettings settings = new KeyphraseExtractorSettings();

    public PalladianKeyphraseExtractor() {
        tokenizer = new TokenizerPlus(settings);
        tokenizer.setUsePosTagging(false);
        // tokenizer.setUsePosTagging(true);
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
        
        // TODO removal of Stopwords should be done before!

        // tokenize the text and add the tokens/phrases to the corpus
        List<Token> tokens = tokenize(text);
        addToCorpus(tokens, keyphrases);

    }
    
    
    public void addToCorpus(List<Token> tokens, Set<String> keyphrases) {

        corpus.addPhrases(tokens);

        // the corpus stores the *stemmed* keyphrases
        keyphrases = stem(keyphrases);
        corpus.addKeyphrases(keyphrases);
        
    }

    // private List<Candidate> trainData = new ArrayList<Candidate>();
    private List<DocumentModel> trainDocuments = new ArrayList<DocumentModel>();
    int trainInstances = 0;

    @Override
    public void startTraining() {
        corpus.clear();
        // trainData.clear();
        trainDocuments.clear();
        trainInstances = 0;
    }
    
    boolean switched = false;

    @Override
    public void train(String inputText, Set<String> keyphrases, int index) {
        
        List<Token> tokens = tokenize(inputText);

        // add the document to the corpus
        addToCorpus(tokens, keyphrases);

        // if (trainData.size() > TRAIN_DATA_LIMIT) {
        if (trainInstances > TRAIN_DATA_LIMIT) {
            if (switched == false) {
                switched = true;
                System.out.println("switched to fast mode...");
                tokenizer.setUsePosTagging(false);
            }
            return;
        }

        // create the document model
        DocumentModel candidates = createDocumentModel(tokens);

        // keep stemmed and unstemmed representation
        Set<String> stemmedKeyphrases = stem(keyphrases);
        keyphrases.addAll(stemmedKeyphrases);

        // mark positive candidates, i.e. those which were manually assigned
        // in the training data
        for (Candidate candidate : candidates) {
            boolean isCandidate = keyphrases.contains(candidate.getStemmedValue());
            isCandidate = isCandidate || keyphrases.contains(candidate.getStemmedValue().replace(" ", ""));
            isCandidate = isCandidate || keyphrases.contains(candidate.getValue());
            isCandidate = isCandidate || keyphrases.contains(candidate.getValue().replace(" ", ""));
            candidate.setPositive(isCandidate);
        }

        // trainData.addAll(candidates);
        trainDocuments.add(candidates);
        trainInstances += candidates.size(); // XXX dirty

    }

    @Override
    public void endTraining() {
        
        // XXX
        corpus.makeRelativeScores();

        // keep the CSV training data in memory for now
        StringBuilder csvBuilder = new StringBuilder();

        // Set<String> featureNames = trainData.iterator().next().getFeatures().keySet();
        Map<String, Object> features = trainDocuments.iterator().next().iterator().next().getFeatures();
        Map<String, Double> numericFeatures = getNumericFeatures(features);
        //Set<String> featureNames = features.keySet();
        Set<String> featureNames = numericFeatures.keySet();
        // trainData.append("#");
        csvBuilder.append(StringUtils.join(featureNames, ";")).append("\n");

        // write all values
        for (DocumentModel trainData : trainDocuments) {
            
            trainData.calculateCorrelations();
            
            for (Candidate candidate : trainData) {
                // Collection<Object> featureValues = candidate.getFeatures().values();
                Collection<Double> featureValues = getNumericFeatures(candidate.getFeatures()).values();
                csvBuilder.append(StringUtils.join(featureValues, ";")); //.append("\n");
                // csvBuilder.append(";").append(candidate.getFeatures().get("positive")).append(";").append("\n");
                csvBuilder.append("\n");
            }
        }

        final String trainDataPath = "data/temp/KeyphraseExtractorTraining.csv";
        FileHelper.writeToFile(trainDataPath, csvBuilder);
        ///// trainData.clear();
        trainDocuments.clear();

        // save memory; this is necessary, as the corpus consumes great amounts of memory, but
        // fortunately we don't need the corpus for the training process
        saveCorpus();
        corpus = null;

        // train a new Classifier using the CSV data from above
        classifier.trainClassifier(trainDataPath, true);

        // save the trained classifier
        saveClassifier();

        // load the corpus again which has been removed from memory
        loadCorpus();

    }

    private void saveCorpus() {
        //String filePath = settings.getModelPath() + "/corpus.ser";
        String filePath = settings.getModelPath() + "/corpus.ser.gz";

        LOGGER.info("saving corpus to " + filePath + " ...");
        StopWatch sw = new StopWatch();
        // corpus.makeRelativeScores(); // XXX
        FileHelper.serialize(corpus, filePath);
        LOGGER.info("saved corpus in " + sw.getElapsedTimeString());
    }

    private void loadCorpus() {
        LOGGER.info("loading corpus ...");
        StopWatch sw = new StopWatch();
        // corpus = FileHelper.deserialize(settings.getModelPath() + "/corpus.ser");
        corpus = FileHelper.deserialize(settings.getModelPath() + "/corpus.ser.gz");

        LOGGER.info("loaded corpus in " + sw.getElapsedTimeString());
    }

    private void saveClassifier() {
        String filePath = settings.getModelPath() + "/classifier.ser";
        LOGGER.info("saving classifier " + filePath + " ...");
        StopWatch sw = new StopWatch();
        
        classifier.saveTrainedClassifier(filePath);
        
        // for debugging; write the classifier as text representation
        String classifierString = classifier.getClassifier().toString();
        FileHelper.writeToFile("data/temp/KeyphraseExtractorClassifier.txt", classifierString);
        
        LOGGER.info("saved classifier in " + sw.getElapsedTimeString());
    }

    private void loadClassifier() {
        LOGGER.info("loading classifier ...");
        StopWatch sw = new StopWatch();
        classifier.loadTrainedClassifier(settings.getModelPath() + "/classifier.ser");
        LOGGER.info("loaded classifier in " + sw.getElapsedTimeString());
    }

    public void load() {
        loadCorpus();
        loadClassifier();
    }

    @Override
    public void startExtraction() {
        load();
    }

    @Override
    public List<Keyphrase> extract(String text) {

        addToCorpus(text);

        DocumentModel candidates = createDocumentModel(text);
        
        /// XXX
        candidates.calculateCorrelations();

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
        classify(candidates);

        // Collections.sort(candidates, new CandidateComparator());
        // for (Candidate candidate : candidates) {
        // LOGGER.debug(candidate.getRegressionValue() + "\t" + candidate);
        // }

        // do the correlation based re-ranking
        reRankCandidates(candidates);

        // create the final result, take the top n candidates
        limitResult(candidates);

        // return candidates;
        List<Keyphrase> keyphrases = new ArrayList<Keyphrase>();
        for (Candidate candidate : candidates) {
            keyphrases.add(new Keyphrase(candidate.getValue(), candidate.getRegressionValue()));
        }

        return keyphrases;

    }

    /**
     * Use the {@link WekaClassifierWrapper} to classify the identified {@link Candidate}s.
     * 
     * @param candidates
     */
    private void classify(DocumentModel candidates) {
        
        // FIXME need to update for nominal attributes.
        
        for (Candidate candidate : candidates) {
            
            Map<String, Double> tmp = getNumericFeatures(candidate.getFeatures());
            
            FeatureObject featureObject = new FeatureObject(tmp);
            double result = classifier.classifySoft(featureObject)[0];
            candidate.setRegressionValue(result);
        }

    }

    private Map<String, Double> getNumericFeatures(Map<String,Object>features) {
        Map<String, Double> tmp = new HashMap<String, Double>();
        

        // tmp. fix.
        // Map<String, Object> features = candidate.getFeatures();
        Set<Entry<String, Object>> entrySet = features.entrySet();
        for (Entry<String, Object> entry : entrySet) {
            if (entry.getValue() instanceof Double) {
                tmp.put(entry.getKey(), (Double) entry.getValue());
            }
            if (entry.getKey().equals("positive")) {
                double value = 0.0;
                if ("positive".equals((String) entry.getValue())) {
                    value = 1.0;
                }
                tmp.put("positive", value);
            }
        }
        return tmp;
    }

    /**
     * Reduces the {@link Candidate}s in the DocumentModel (e. g. the list of {@link Candidate}s) using the specified
     * {@link AssignmentMode}.
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
     * TODO add length (e.g. num-of-terms-) re-ranking?
     * 
     * @param candidates
     */
    private void reRankCandidates(DocumentModel candidates) {

        StopWatch sw = new StopWatch();

        if (candidates.isEmpty()) {
            return;
        }

        Collections.sort(candidates, new CandidateComparator());
        
        ////////////////////////////////////////////////////////

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
                    currentCandidate.increaseRegressionValue(settings.getCorrelationWeight()
                            * correlation.getRelativeCorrelation());
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
            float factor = settings.getCorrelationWeight() / numReRanking;

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

        List<Token> tokens = tokenize(text);
        return createDocumentModel(tokens);

    }
    
    private DocumentModel createDocumentModel(List<Token> tokens) {
        
        DocumentModel model = new DocumentModel(corpus);
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

        if (settings.getMinPhraseLength() == 1) {
            tokens.addAll(uniGrams);
        }

        if (!tokenizer.isUsePosTagging()) { // XXX
            List<Token> collocations = tokenizer.makeCollocations(uniGrams, settings.getMinPhraseLength(), settings.getMaxPhraseLength());
            tokens.addAll(collocations);
        }
        
        //        Set<String> stopwords = settings.getStopwords();
        //        ListIterator<Token> lit = tokens.listIterator();
        //        while (lit.hasNext()) {
        //            Token current = lit.next();
        //            if (stopwords.contains(current.getUnstemmedValue())) {
        //                lit.remove();
        //            }
        //        }

        return tokens;

    }

    /**
     * Stem a term or a phrase using the specified stemmer. For phrases (e. g. multiple terms separated by space
     * characters) each single term is stemmed, stopwords removed and the all final stems sorted alphabetically.
     * 
     * @param unstemmed
     * @return
     */
    private String stem(String unstemmed) {
        
        List<String> result = new ArrayList<String>();
        SnowballStemmer stemmer = settings.getStemmer();
        Set<String> stopwords = settings.getStopwords();

        // stem each part of the phrase
        String[] parts = unstemmed.toLowerCase().split(" ");
        for (String part : parts) {
            stemmer.setCurrent(part);
            stemmer.stem();
            String current = stemmer.getCurrent();
            if (!stopwords.contains(current)) {
                result.add(current);
            }
        }
        
        // sort stems alphabetically
        Collections.sort(result);
        return StringUtils.join(result, " ");

    }

    /**
     * Stem a set of terms/phrases (see {@link #stem(String)}.
     * 
     * @param unstemmed
     * @return
     */
    private Set<String> stem(Set<String> unstemmed) {
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

    @SuppressWarnings("unused")
    public static void main(String[] args) {
        
        
        final PalladianKeyphraseExtractor extractor = new PalladianKeyphraseExtractor();
        // System.out.println(extractor.stem("the quick brown fox jumps over the lazy dog"));
        extractor.load();
        // System.out.println(extractor.corpus.toString());
        System.exit(0);
        

        KeyphraseExtractorSettings extractorSettings = extractor.getSettings();
        extractorSettings.setAssignmentMode(AssignmentMode.COMBINED);
        extractorSettings.setReRankingMode(ReRankingMode.NO_RERANKING);
        extractorSettings.setMinOccurenceCount(1);
        extractorSettings.setKeyphraseCount(10);
        extractorSettings.setKeyphraseThreshold(0.3f);

        // String filePath = "data/tagData_shuf_10000aa";
        String filePath = "/Users/pk/Dropbox/tmp/tagData_shuf_10000aa";
        // String filePath = "fao_splitaa";
        // // String classPath = "classifier_fao.ser";
        String classPath = "bagging_classifier.ser";

        // String classPath = "neuralnet_classifier.ser";

        extractorSettings.setModelPath("data/corpus_model.ser");
        // extractorSettings.setAssignmentMode(AssignmentMode.COMBINED);
        // extractorSettings.setReRankingMode(ReRankingMode.DEEP_CORRELATION_RERANKING);
        // extractorSettings.setCorrelationWeight(50000);
        // extractorSettings.setControlledMode(false);
        extractorSettings.setControlledMode(true);

        // //////////////////////////////////////////////
        // CORPUS CREATION
        // //////////////////////////////////////////////
        // extractor.buildCorpus(filePath);
        // extractor.loadCorpus();

        // //////////////////////////////////////////////
        // FEATURE SET FOR TRAINING CREATION
        // //////////////////////////////////////////////
        // extractor.buildClassifier(filePath, 750);
        // extractor.saveClassifier(classPath);
        // System.exit(0);

        // //////////////////////////////////////////////
        // EVALUATION
        // //////////////////////////////////////////////

        // extractor.loadClassifier(classPath);
        extractor.loadCorpus();
        // extractor.evaluate("fao_splitab", 500);
        // extractor.evaluate("/Users/pk/Dropbox/tmp/tagData_shuf_10000ab", 1000);

        // extractor.getSettings().setReRankingMode(ReRankingMode.NO_RERANKING);
        // extractor.evaluate("fao_splitab", 100);

        // String stem = extractor.stem("the quick brown foxes jumps over the lazy dogs.");
        // System.out.println(stem);
        // System.exit(0);
        // Crawler c = new Crawler();
        // String result = c.download("http://www.i-funbox.com/");
        // result = HTMLHelper.htmlToString(result, true);
        // extractor.getSettings().setKeyphraseCount(20);
        // List<Candidate> extract = extractor.extract(result);
        // System.out.println(extract);

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

    @Override
    public boolean needsTraining() {
        return true;
    }

    @Override
    public String getExtractorName() {
        return "Palladian Keyphrase Extractor";
    }

}
