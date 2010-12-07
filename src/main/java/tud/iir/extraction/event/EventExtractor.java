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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import tud.iir.classification.Classifier;
import tud.iir.classification.FeatureObject;
import tud.iir.daterecognition.DateGetterHelper;
import tud.iir.daterecognition.dates.ContentDate;
import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.extraction.entity.ner.Annotation;
import tud.iir.extraction.entity.ner.Annotations;
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
public class EventExtractor {
    // 8/6/10 4:20 PM

    /* the instance of this class */
    private static final EventExtractor INSTANCE = new EventExtractor();

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(EventExtractor.class);

    private WhereClassifier whereClassifier;
    private WhoClassifier whoClassifier;

    private EventFeatureExtractor featureExtractor;

    private final String MODEL_WHERE;
    private final String MODEL_WHO;

    // private boolean deepMode = true;

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
        featureExtractor = new EventFeatureExtractor();

        PropertiesConfiguration config = null;

        try {
            config = new PropertiesConfiguration("config/models.conf");
        } catch (final ConfigurationException e) {
            LOGGER.error("could not get model path from config/models.conf, "
                    + e.getMessage());
        }

        if (config != null) {
            MODEL_WHO = config.getString("models.palladian.en.event.who");
            MODEL_WHERE = config.getString("models.palladian.en.event.where");

            setWhoClassifier(Classifier.LINEAR_REGRESSION);
            setWhereClassifier(Classifier.LINEAR_REGRESSION);
        } else {
            MODEL_WHO = "";
            MODEL_WHERE = "";
        }

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

            event = new Event(pce.getResultTitle(), StringHelper
                    .makeContinuousText(pce.getResultText()), url);

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
        extractWhy(event);
        extractWhen(event);
        extractHow(event);

        LOGGER.info("who:   " + event.getWho());
        LOGGER.info("what:  " + event.getWhat());
        LOGGER.info("where: " + event.getWhere());
        LOGGER.info("when:  " + event.getWhen());
        LOGGER.info("why:   " + event.getWhy());
        LOGGER.info("how:   " + event.getHow());

    }

    /**
     * extracts the when from an given event
     * 
     * @param event
     */
    public void extractWhen(Event event) {

        /*
         * final DateGetter dg = new DateGetter(event.getUrl());
         * dg.setAllFalse(); dg.setTechHTMLContent(true);
         * dg.setTechHTMLHead(true); dg.setTechHTTP(true); dg.setTechURL(true);
         * CollectionHelper.print(dg.getDate());
         */

        final ArrayList<ContentDate> dates = DateGetterHelper
                .findALLDates(event.getText());

        try {
            LOGGER.info("when:" + dates.get(0).getNormalizedDate());
            event.setWhen(dates.get(0).getNormalizedDate().toString());
        } catch (final Exception e) {
            LOGGER.error(e);
        }

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

            // only accept location entities for where slot
            if (fo.getFeature("type").equals(
                    EventFeatureExtractor.CATEGORY_LOCATION)) {
                result = whereClassifier.classifySoft(fo);

                rankedCandidates.put(longestTerm(entry.getKey()), result[0]);
            }

        }

        rankedCandidates = sortByValue(rankedCandidates);
        final Object[] keys = rankedCandidates.keySet().toArray();
        final Object[] values = rankedCandidates.values().toArray();

        if (rankedCandidates.size() > 0) {
            LOGGER.info("highest ranked where:" + keys[0] + "(" + values[0]
                    + ")");
            event.setWhereCandidates(rankedCandidates);
            event.setWhere(rankedCandidates.keySet().toArray()[0].toString());
        } else {
            LOGGER.info("no WHERE was found.");
        }
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

        final Map<String, Double> whyCandidates = new HashMap<String, Double>();

        final String text = StringHelper.makeContinuousText(event.getText());
        String whatVerb = null;

        if (event.getWhat() != null) {
            final String tagged = featureExtractor.getPOSTags(event.getWhat())
                    .getTaggedString();

            // extracting the verb from whatPhrase
            try {
                final Pattern p = Pattern.compile("(.*?)/VB(.*)");

                final Matcher m = p.matcher(tagged);

                while (m.find()) {
                    for (int i = 1; i <= m.groupCount(); i++) {
                        LOGGER.info(m.group(i) + " (" + m.start(i) + ","
                                + m.end(i) + ")");
                        if (whatVerb == null) {
                            whatVerb = m.group(i);
                        }
                    }
                }
            } catch (final PatternSyntaxException e) {
                LOGGER.error("Regex syntax error: " + e.getMessage());
                LOGGER.error("Error description: " + e.getDescription());
                LOGGER.error("Error index: " + e.getIndex());
                LOGGER.error("Erroneous pattern: " + e.getPattern());
                return;
            }
        }
        // @TODO: better weighting on the result .. because should rate higher
        // than whatVerb+
        final String who = event.getWho();
        // System.out.println("who:" + who + ",what:" + event.getWhat() + "("
        // + whatVerb + ")");

        final Map<String, Double> regExpMap = getWhyRegExp(who, whatVerb);
        String taggedStc = null;
        for (final String stc : featureExtractor.getSentences(text)) {
            taggedStc = featureExtractor.getPOSTags(stc).getTaggedString();
            for (final Entry<String, Double> entry : regExpMap.entrySet()) {

                final Pattern pattern = Pattern.compile(entry.getKey());
                Double confidence = entry.getValue();
                confidence += StringHelper.calculateSimilarity(stc, who + " "
                        + whatVerb);
                final Matcher mToMatcher = pattern.matcher(taggedStc);
                while (mToMatcher.find()) {
                    whyCandidates.put(stc, confidence);
                }
            }
        }

        // CollectionHelper.print(whyCandidates);
        if (whyCandidates.size() > 0) {
            event.setWhyCandidates(whyCandidates);
            event.setWhy(whyCandidates.keySet().toArray()[0].toString());

        } else {
            LOGGER.info("no why found");
        }
    }

    private Map<String, Double> getWhyRegExp(String who, String what) {

        final Map<String, Double> regExpMap = new HashMap<String, Double>();

        regExpMap.put("(" + what + "(.*)to/TO [^ \t](.*)/VB)", 0.5);
        // regExpMap.put("(" + who + "(.*)to/TO [^ \t](.*)/VB)", 0.5);
        regExpMap.put("(" + what + "(.*)will)", 0.5);
        regExpMap.put("(since)", 0.2);
        regExpMap.put("(cause)", 0.2);
        regExpMap.put("(because)", 0.2);
        regExpMap.put("(hence)", 0.2);
        regExpMap.put("(therefore)", 0.2);
        regExpMap.put("(why)", 0.2);
        regExpMap.put("(result)", 0.2);
        regExpMap.put("(reason)", 0.2);
        regExpMap.put("(provide)", 0.1);
        regExpMap.put("('s behind)", 0.2);
        regExpMap.put("(Due to)", 0.2);

        return regExpMap;
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
        boolean changed = false;
        int i = 0;

        // iterating throug whoCandidates until a subsequent verbPhrase is found
        // in title
        while (titleVerbPhrase == null
                && i < event.getWhoCandidates().keySet().size()) {
            who = (String) event.getWhoCandidates().keySet().toArray()[i];

            /*
             * String[] parts = null; if (who.contains(" ")) { parts =
             * who.split(" "); } else { parts = new String[] { who }; }
             */

            if (!changed) {
                titleVerbPhrase = getSubsequentVerbPhrase(event.getTitle(), who);
                if (titleVerbPhrase != null && !changed) {
                    event.setWho(who);
                    LOGGER.info("new who:" + who);
                    changed = true;
                    // System.out.println("new who: " + who);
                }

            }

            i++;
        }

        // if no titleVerbPhrase was found explore the text
        if (titleVerbPhrase == null) {

            // finding sentences containing the who
            for (final String stc : featureExtractor.getSentences(event
                    .getText())) {

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

        final ArrayList<String> verbPhrases = new ArrayList<String>();
        boolean foundNoun = false;
        // boolean foundVerb = false;
        for (final TagAnnotation tagAnnotation : featureExtractor
                .getParse(sentence)) {

            if (foundNoun && tagAnnotation.getTag().contains("V")
                    && tagAnnotation.getChunk().length() < 100) {
                verbPhrases.add(tagAnnotation.getChunk());
                // foundVerb = true;
                foundNoun = false;

            }
            if (tagAnnotation.getTag().contains("N")
                    && tagAnnotation.getChunk().contains(word)) {

                foundNoun = true;
            }
            /*
             * if (foundVerb && tagAnnotation.getTag().contains("NN")) {
             * verbPhrases.set(0, verbPhrases.get(0) +
             * tagAnnotation.getChunk()); }
             */
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

    public String shortestTerm(Annotations annotations) {

        String term = "longlonglonglonglonglong";

        for (final Annotation annotation : annotations) {
            if (term.length() > annotation.getLength()) {
                term = annotation.getEntity().getName();
            }

        }
        return term;

    }

    /**
     * Extracts the How from a given event.
     * 
     * @param event
     */
    public void extractHow(Event event) {
        // pattern matching "like..."

        Map<String, Double> rankedCandidates = new HashMap<String, Double>();

        final String text = StringHelper.makeContinuousText(event.getText());
        for (final String stc : featureExtractor.getSentences(text)) {

            double confidence = StringHelper.calculateSimilarity(stc, event
                    .getTitle());

            if (stc.contains("like")) {
                confidence = confidence + 1.0;
            }
            if (confidence > 2.0) {
                rankedCandidates.put(stc, confidence);
            }
        }

        rankedCandidates = sortByValue(rankedCandidates);
        if (rankedCandidates.size() > 0) {
            event.setHowCandidates(rankedCandidates);
            event.setHow(rankedCandidates.keySet().toArray()[0].toString());
        }

    }

    /**
     * extracts the who and whoCandidates from an given event.
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

            // put in the shortest term of the annotations found in text
            rankedCandidates.put(longestTerm(entry.getKey()), result[0]);

        }

        rankedCandidates = sortByValue(rankedCandidates);
        final Object[] values = rankedCandidates.keySet().toArray();
        final Object[] keys = rankedCandidates.values().toArray();

        if (rankedCandidates.size() > 0) {
            LOGGER
                    .info("highest ranked who:" + values[0] + "(" + keys[0]
                            + ")");

            event.setWhoCandidates(rankedCandidates);
            event.setWho(values[0].toString());
        }

    }

    /**
     * Returns the Where Classifier
     * 
     * @return
     */
    public WhereClassifier getWhereClassifier() {
        return whereClassifier;
    }

    /**
     * Setter of WhereClassifier.
     * 
     * @param type
     */
    public void setWhereClassifier(int type) {
        whereClassifier = new WhereClassifier(type);
        whereClassifier.useTrainedClassifier(MODEL_WHERE);
    }

    /**
     * Getter of WhoClassifier
     * 
     * @return
     */
    public WhoClassifier getWhoClassifier() {
        return whoClassifier;
    }

    /**
     * Setter of WhoClassifier
     * 
     * @param type
     */
    public void setWhoClassifier(int type) {
        whoClassifier = new WhoClassifier(type);
        whoClassifier.useTrainedClassifier(MODEL_WHO);
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

            final String title = fields[1];
            final String who = fields[2];
            final String where = fields[3];
            final String what = fields[4];
            final String why = fields[5];
            final String how = fields[6];

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

        LOGGER.setLevel(Level.ALL);
        System.setProperty("wordnet.database.dir",
                "/usr/local/WordNet-3.0/dict");

        final EventExtractor eventExtractor = EventExtractor.getInstance();

        eventExtractor.setWhoClassifier(Classifier.LINEAR_REGRESSION);
        eventExtractor.setWhereClassifier(Classifier.LINEAR_REGRESSION);

        final Event event = EventExtractor
                .extractEventFromURL("http://www.bbc.co.uk/news/world-asia-pacific-11817826");

        eventExtractor.getFeatureExtractor().setFeatures(event);

        // evaluateEvents();

        LOGGER.info(eventExtractor.getFeatureExtractor().getPOSTags(
                event.getTitle()).getTaggedString());

        // eventExtractor.extractWho(event);
        // eventExtractor.extractWhat(event);
        // eventExtractor.extractWhere(event);
        // eventExtractor.extractWhy(event);
        // eventExtractor.extractWhen(event);
        // eventExtractor.extractHow(event);

        eventExtractor.extract5W1H(event);

    }
}
