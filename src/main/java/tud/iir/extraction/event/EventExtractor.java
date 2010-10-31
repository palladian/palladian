package tud.iir.extraction.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import tud.iir.daterecognition.DateGetter;
import tud.iir.extraction.Extractor;
import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.extraction.entity.ner.Annotation;
import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;
import weka.core.stemmers.SnowballStemmer;

import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * Event Extractor
 * 
 * @author Martin Wunderwald
 */
public class EventExtractor extends Extractor {
    // 8/6/10 4:20 PM

    /* the instance of this class */
    private static final EventExtractor INSTANCE = new EventExtractor();

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(EventExtractor.class);

    private WhereClassifier whereClassifier;
    private WhoClassifier whoClassifier;

    private EventFeatureExtractor featureExtractor;

    /**
     * @return EventExtractor
     */
    public static EventExtractor getInstance() {
        return INSTANCE;
    }

    /**
     * construtor of this class
     */
    protected EventExtractor() {
        super();
        // do not analyze any binary files
        addSuffixesToBlackList(Extractor.URL_BINARY_BLACKLIST);
        featureExtractor = new EventFeatureExtractor();
    }

    public void startExtraction() {
        startExtraction(true);
    }

    public void startExtraction(boolean continueFromLastExtraction) {

        // reset stopped command
        setStopped(false);

    }

    @Override
    protected void saveExtractions(boolean saveExtractions) {

    }

    /**
     * extracts an event from given url
     * 
     * @param url
     *            - url of a news article
     * @return Event - The event
     */
    public static Event extractEventFromURL(String url) {

        Event event = null;
        try {

            final PageContentExtractor pce = new PageContentExtractor();
            pce.setDocument(url);

            event = new Event(pce.getResultTitle(), pce.getResultText(), url);

        } catch (final PageContentExtractorException e) {

            LOGGER.error(e);
            LOGGER.error("URL not found: " + url);

        }
        return event;
    }

    /**
     * extracts the 5W1H from an given event
     * 
     * @param event
     */
    public void extract5W1H(Event event) {

        extractWho(event);
        extractWhat(event);
        extractWhere(event);

        // extractWhy(event);
        // extractWhen(event);

    }

    /**
     * extracts the when from an given event
     * 
     * @param event
     */
    public void extractWhen(Event event) {

        final DateGetter dg = new DateGetter(event.getUrl());
        dg.setAllFalse();
        dg.setTechHTMLContent(true);
        dg.setTechHTMLHead(true);
        dg.setTechHTTP(true);
        dg.setTechURL(true);
        CollectionHelper.print(dg.getDate());

    }

    /**
     * extracts the where from an given event
     * 
     * @param event
     */
    public void extractWhere(Event event) {

        final Map<Annotations, FeatureObject> features = event
                .getAnnotationFeatures();

        double[] result = null;

        Map<String, Double> rankedCandidates = new HashMap<String, Double>();

        for (final Entry<Annotations, FeatureObject> entry : features
                .entrySet()) {

            final FeatureObject fo = entry.getValue();

            result = whereClassifier.classifySoft(fo);

            rankedCandidates.put(longestTerm(entry.getKey()), result[0]);

        }

        rankedCandidates = sortByValue(rankedCandidates);
        final Object[] values = rankedCandidates.keySet().toArray();
        final Object[] keys = rankedCandidates.values().toArray();

        LOGGER.info("highest ranked wheres:" + values[0] + "(" + keys[0] + "),"
                + values[1] + "(" + keys[1] + ")");

        event.setWhereCandidates(rankedCandidates);
        event.setWhere(values[0].toString());

    }

    static Map<String, Double> sortByValue(Map<String, Double> map) {

        final List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(
                map.entrySet());
        Collections.sort(list, new Comparator<Entry<String, Double>>() {
            public int compare(Entry<String, Double> obj1,
                    Entry<String, Double> obj2) {
                return ((Comparable<Double>) obj1.getValue()).compareTo(obj2
                        .getValue());
            }
        });

        Collections.reverse(list);

        final Map<String, Double> result = new LinkedHashMap<String, Double>();
        for (final Iterator<Entry<String, Double>> it = list.iterator(); it
                .hasNext();) {
            final Entry<String, Double> entry = it.next();
            result.put(entry.getKey(), entry.getValue());
        }

        return result;
    }

    /**
     * extracts the why from an given event
     * 
     * @param event
     */
    public void extractWhy(Event event) {
        event.setWhy("?");
    }

    public List<String> longestSubstrings(String str1, String str2) {

        final String s1 = StringHelper.removeStopWords(str1);
        final String s2 = StringHelper.removeStopWords(str2);

        final TokenizerFactory mTokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        final Tokenizer tokenizer1 = mTokenizerFactory.tokenizer(s1
                .toCharArray(), 0, s1.length());
        final Tokenizer tokenizer2 = mTokenizerFactory.tokenizer(s2
                .toCharArray(), 0, s2.length());

        final List<String> a1 = new ArrayList<String>();
        final List<String> a2 = new ArrayList<String>();
        final List<String> w1 = new ArrayList<String>();
        final List<String> w2 = new ArrayList<String>();

        tokenizer1.tokenize(a1, w1);
        tokenizer2.tokenize(a2, w2);

        // CollectionHelper.print(a1);
        final SnowballStemmer ss = new SnowballStemmer();

        for (int i = 0; i < a1.size(); i++) {
            a1.set(i, ss.stem(a1.get(i).toLowerCase().trim()));
        }
        for (int i = 0; i < a2.size(); i++) {
            a2.set(i, ss.stem(a2.get(i).toLowerCase().trim()));
        }

        if (a1.size() > a2.size()) {
            a1.retainAll(a2);
            return a1;
        } else {
            a2.retainAll(a1);
            return a2;
        }

    }

    /**
     * extracts the what from an given event
     * 
     * @param event
     */
    public void extractWhat(Event event) {

        String what = null;

        final String text = StringHelper.makeContinuousText(event.getText());
        final String title = StringHelper.makeContinuousText(event.getTitle());

        LOGGER.info("title: " + title);
        LOGGER.info("text: " + text);

        // event.setWho("police");

        String titleVerbPhrase = null;
        String who;
        int i = 0;

        // iterating throug whoCandidates until a subsequent verbPhrase is found
        // in title
        while (titleVerbPhrase == null
                && i < event.getWhoCandidates().keySet().size()) {
            who = (String) event.getWhoCandidates().keySet().toArray()[i];
            titleVerbPhrase = getSubsequentVerbPhrase(event.getTitle(), who);
            if (titleVerbPhrase != null) {
                event.setWho(who);
                LOGGER.info("new who:" + who);
            }
            i++;
        }

        // if no titleVerbPhrase was found explore the text
        if (titleVerbPhrase == null) {

            // final ArrayList<String> whoSentences = new ArrayList<String>();

            // final HashSet<String> longestSubstrings = new HashSet<String>();
            // ArrayList<String> longestSubstrings = new ArrayList<String>();
            // final Map<String, Double> overlap = new HashMap<String,
            // Double>();
            // finding sentences containing the who
            for (final String stc : featureExtractor.getSentences(event
                    .getText())) {

                /*
                 * // longest common substrings between title and first sentence
                 * overlap.put(stc, StringHelper.calculateSimilarity(stc,
                 * title)); longestSubstrings.addAll(longestSubstrings(stc,
                 * title)); final double sim =
                 * StringHelper.calculateSimilarity(event .getWho(), stc); if
                 * (sim > 0.5) { }
                 */

                if (stc.contains(event.getWho()) && what == null) {
                    what = getSubsequentVerbPhrase(stc, event.getWho());
                }

            }
        } else {
            what = titleVerbPhrase;

        }

        LOGGER.info("most likely what: " + what);
        event.setWhat(what);

    }

    /**
     * @param sentence
     * @param word
     * @return
     */
    private String getSubsequentVerbPhrase(String sentence, String word) {

        final OpenNLPParser onlp = new OpenNLPParser();
        onlp.loadModel();
        onlp.parse(sentence, 0);

        final ArrayList<String> verbPhrases = new ArrayList<String>();
        boolean foundNoun = false;
        for (final TagAnnotation tagAnnotation : onlp.getTagAnnotations()) {

            if (foundNoun && tagAnnotation.getTag().contains("VP")
                    && tagAnnotation.getChunk().length() < 100) {

                verbPhrases.add(tagAnnotation.getChunk());
                foundNoun = false;

            }
            if (tagAnnotation.getTag().contains("N")
                    && tagAnnotation.getChunk().contains(word)) {

                foundNoun = true;
            }
        }
        return (verbPhrases.size() > 0) ? verbPhrases.get(0) : null;
    }

    public String longestTerm(Annotations annotations) {

        String term = "";

        for (final Annotation annotation : annotations) {
            if (term.length() < annotation.getLength()) {
                term = annotation.getEntity().getName();

            }

        }
        return term;

    }

    /**
     * extracts the who from an given event
     * 
     * @param event
     */
    public void extractWho(Event event) {

        final Map<Annotations, FeatureObject> features = event
                .getAnnotationFeatures();

        double[] result = null;

        Map<String, Double> rankedCandidates = new HashMap<String, Double>();

        for (final Entry<Annotations, FeatureObject> entry : features
                .entrySet()) {

            final FeatureObject fo = entry.getValue();

            result = whoClassifier.classifySoft(fo);

            rankedCandidates.put(longestTerm(entry.getKey()), result[0]);

        }

        rankedCandidates = sortByValue(rankedCandidates);
        final Object[] values = rankedCandidates.keySet().toArray();
        final Object[] keys = rankedCandidates.values().toArray();

        if (keys.length > 1) {
            LOGGER.info("highest ranked whos:" + values[0] + "(" + keys[0]
                    + ")," + values[1] + "(" + keys[1] + ")");
        }
        event.setWhoCandidates(rankedCandidates);
        event.setWho(values[0].toString());

    }

    public WhereClassifier getWhereClassifier() {
        return whereClassifier;
    }

    public void setWhereClassifier(int type) {
        whereClassifier = new WhereClassifier(type);
        whereClassifier
                .useTrainedClassifier("data/learnedClassifiers/where.model");
    }

    public WhoClassifier getWhoClassifier() {
        return whoClassifier;
    }

    public void setWhoClassifier(int type) {
        whoClassifier = new WhoClassifier(type);
        whoClassifier.useTrainedClassifier("data/learnedClassifiers/who.model");
    }

    /**
     * @return the featureExtractor
     */
    public EventFeatureExtractor getFeatureExtractor() {
        return featureExtractor;
    }

    /**
     * @param featureExtractor
     *            the featureExtractor to set
     */
    public void setFeatureExtractor(EventFeatureExtractor featureExtractor) {
        this.featureExtractor = featureExtractor;
    }

    @SuppressWarnings("unused")
    private void evaluateEvents() {
        final Map<Integer, String[]> events = featureExtractor
                .readCSV("data/news_articles.csv");
        final EventExtractor eventExtractor = EventExtractor.getInstance();
        eventExtractor.setWhoClassifier(Classifier.LINEAR_REGRESSION);
        // eventExtractor.setWhereClassifier(Classifier.LINEAR_REGRESSION);

        for (final Entry<Integer, String[]> entry : events.entrySet()) {
            final String[] fields = entry.getValue();
            // int id = entry.getKey();

            final String url = fields[0];

            // final String title = fields[1];
            final String who = fields[2];
            // final String where = fields[3];
            // final String what = fields[4];
            // final String why = fields[5];
            // final String how = fields[6];

            final Event event = EventExtractor.extractEventFromURL(url);
            featureExtractor.setFeatures(event);
            eventExtractor.extractWho(event);

            LOGGER.info("WHO: " + event.getWho() + " / " + who);
        }

    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        System.setProperty("wordnet.database.dir",
                "/usr/local/WordNet-3.0/dict");

        final EventExtractor eventExtractor = EventExtractor.getInstance();

        eventExtractor.setWhoClassifier(Classifier.LINEAR_REGRESSION);
        eventExtractor.setWhereClassifier(Classifier.LINEAR_REGRESSION);

        final Event event = EventExtractor
                .extractEventFromURL("http://www.bbc.co.uk/news/world-latin-america-11657248");
        eventExtractor.getFeatureExtractor().setFeatures(event);

        final StopWatch sw = new StopWatch();
        sw.start();

        // evaluateEvents();
        eventExtractor.extractWho(event);
        eventExtractor.extractWhat(event);
        eventExtractor.extractWhere(event);

        sw.stop();
        LOGGER.info("time elapsed: " + sw.getElapsedTimeString());

    }
}
