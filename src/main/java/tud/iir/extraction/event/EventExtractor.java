package tud.iir.extraction.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

import com.aliasi.chunk.Chunk;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * Event Extractor
 * 
 * @author Martin Wunderwald
 */
public class EventExtractor extends Extractor { // NOPMD by Martin Wunderwald on
    // 8/6/10 4:20 PM

    /* the instance of this class */
    private static final EventExtractor INSTANCE = new EventExtractor();

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(EventExtractor.class);

    private WhereClassifier whereClassifier;
    private WhoClassifier whoClassifier;

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

        } catch (PageContentExtractorException e) {

            LOGGER.error(e);
            LOGGER.error("URL not found: " + url);

        }
        return event;
    }

    public void extract5W(Event event) {

        extractWho(event);
        extractWhere(event);
        extractWhat(event);
        // extractWhy(event);
        // extractWhen(event);

    }

    /**
     * extracts the when from an given event
     * 
     * @param event
     */
    public void extractWhen(Event event) {

        DateGetter dg = new DateGetter(event.getUrl());
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

        Map<Integer, FeatureObject> features = event.getEntityFeatures();
        Map<Integer, Annotations> annotations = event.getEntityAnnotations();

        double[] result = null;

        Map<String, Double> rankedCandidates = new HashMap<String, Double>();

        for (Entry<Integer, FeatureObject> entry : features.entrySet()) {

            FeatureObject fo = entry.getValue();

            result = whereClassifier.classifySoft(fo);

            rankedCandidates.put(longestTerm(annotations.get(entry.getKey())),
                    result[0]);

        }

        rankedCandidates = sortByValue(rankedCandidates);
        Object[] values = rankedCandidates.keySet().toArray();
        Object[] keys = rankedCandidates.values().toArray();

        LOGGER.info("highest ranked wheres:" + values[0] + "(" + keys[0] + "),"
                + values[1] + "(" + keys[1] + ")");

        event.setWhere(values[0].toString());

    }

    static Map<String, Double> sortByValue(Map<String, Double> map) {

        List<Entry<String, Double>> list = new LinkedList<Entry<String, Double>>(
                map.entrySet());
        Collections.sort(list, new Comparator<Entry<String, Double>>() {
            public int compare(Entry<String, Double> o1,
                    Entry<String, Double> o2) {
                return ((Comparable<Double>) ((Entry<String, Double>) (o1))
                        .getValue()).compareTo(((Entry<String, Double>) (o2))
                        .getValue());
            }
        });

        Collections.reverse(list);

        Map<String, Double> result = new LinkedHashMap<String, Double>();
        for (Iterator<Entry<String, Double>> it = list.iterator(); it.hasNext();) {
            Entry<String, Double> entry = (Entry<String, Double>) it.next();
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

        String s1 = StringHelper.removeStopWords(str1.replaceAll("å", "")
                .replaceAll("‰", ""));
        String s2 = StringHelper.removeStopWords(str2.replaceAll("å", "")
                .replaceAll("‰", ""));

        TokenizerFactory mTokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        Tokenizer tokenizer1 = mTokenizerFactory.tokenizer(s1.toCharArray(), 0,
                s1.length());
        Tokenizer tokenizer2 = mTokenizerFactory.tokenizer(s2.toCharArray(), 0,
                s2.length());

        final List<String> a1 = new ArrayList<String>();
        final List<String> a2 = new ArrayList<String>();
        final List<String> w1 = new ArrayList<String>();
        final List<String> w2 = new ArrayList<String>();

        tokenizer1.tokenize(a1, w1);
        tokenizer2.tokenize(a2, w2);

        // CollectionHelper.print(a1);
        SnowballStemmer ss = new SnowballStemmer();

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

        String text = StringHelper.makeContinuousText(event.getText());
        String title = StringHelper.makeContinuousText(event.getTitle());
        // LOGGER.info("title: " + title);

        // event.setWho("police");

        // EventFeatureExtractor.tagPOS(title);

        Set<Chunk> sentenceChunks = EventFeatureExtractor
                .getSentenceChunks(text);

        Set<Chunk> whoSentences = new HashSet<Chunk>();

        HashSet<String> longestSubstrings = new HashSet<String>();
        // ArrayList<String> longestSubstrings = new ArrayList<String>();
        Map<String, Double> overlap = new HashMap<String, Double>();
        // finding sentences containing the who
        for (Chunk sentence : sentenceChunks) {
            String stc = text.substring(sentence.start(), sentence.end());

            // longest common substrings between title and first sentence
            overlap.put(stc, StringHelper.calculateSimilarity(stc, title));
            longestSubstrings.addAll(longestSubstrings(stc, title));

            double sim = StringHelper.calculateSimilarity(event.getWho(), stc);
            if (sim > 0.5) {
                whoSentences.add(sentence);
            }
        }

        // count occurences of title words in text
        for (String w : longestSubstrings) {
            if (w.length() > 3) {
                // LOGGER.info(w + ":" + StringHelper.countOccurences(text, w,
                // true));
            }
        }

        // LOGGER.info(longestSubstrings.toString());
        // Object[] overlaps = sortByValue(overlap).keySet().toArray();
        // LOGGER.info(overlaps[0]);

        String what = "";
        // int pos = title.length();
        /*
         * for (Chunk titleChunk : EventFeatureExtractor.getPhraseChunks(title +
         * ".")) { String word = title.substring(titleChunk.start(),
         * titleChunk.end()); // LOGGER.info(word + ":" + titleChunk.start() +
         * titleChunk.type()); if (titleChunk.type().equals("verb") &&
         * titleChunk.start() < pos) { pos = titleChunk.start(); what = word; }
         * }
         */
        EventFeatureExtractor.getPhraseChunks(title);

        SnowballStemmer ss = new SnowballStemmer();
        LOGGER.info("in title verb: " + what + "(" + ss.stem(what) + ")");

        // Object[] chunks = sentenceChunks.toArray();
        // Chunk first = (Chunk) chunks[0];
        // Chunk second = (Chunk) chunks[1];
        // String firstSentence = text.substring(first.start(), first.end());
        // String secondSentence = text.substring(second.start(), second.end());
        // LOGGER.info("first sentence: " + firstSentence);
        // LOGGER.info("second sentence: " + secondSentence);

        // LOGGER.info(getSubsequentPhrase(firstSentence, event.getWho()));
        // LOGGER.info(getSubsequentPhrase(secondSentence, event.getWho()));
        LOGGER.info(getSubsequentPhrase(title, event.getWho()));

        // AlchemyNER an = new AlchemyNER();
        // CollectionHelper.print(an.getAnnotations(title));

    }

    private String getSubsequentPhrase(String sentence, String word) {

        String verb = sentence;

        // Iterator<Chunk> it = phraseChunks.iterator();

        /*
         * while (it.hasNext() && CONTINUE_TAGS.contains(((Chunk)
         * it.next()).type())) {
         * 
         * }
         */
        return verb;
    }

    public String longestTerm(Annotations annotations) {

        String term = "";

        for (Annotation annotation : annotations) {
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

        Map<Integer, FeatureObject> features = event.getEntityFeatures();
        Map<Integer, Annotations> annotations = event.getEntityAnnotations();

        double[] result = null;

        Map<String, Double> rankedCandidates = new HashMap<String, Double>();

        for (Entry<Integer, FeatureObject> entry : features.entrySet()) {

            FeatureObject fo = entry.getValue();

            result = whoClassifier.classifySoft(fo);

            rankedCandidates.put(longestTerm(annotations.get(entry.getKey())),
                    result[0]);

        }

        rankedCandidates = sortByValue(rankedCandidates);
        Object[] values = rankedCandidates.keySet().toArray();
        Object[] keys = rankedCandidates.values().toArray();

        if (keys.length > 1) {
            LOGGER.info("highest ranked whos:" + values[0] + "(" + keys[0]
                    + ")," + values[1] + "(" + keys[1] + ")");
        }

        event.setWho(values[0].toString());

    }

    @SuppressWarnings(value = { "unused" })
    private static void trySRL() {

        /*
         * LexicalizedParser lp = new LexicalizedParser(
         * "data/models/englishPCFG.ser.gz", new Options());
         * lp.setOptionFlags(new String[] { "-maxLength", "80",
         * "-retainTmpSubcategories" });
         * 
         * // String[] sent = lp.{ "This", "is", "an", "easy", "sentence", "."
         * }; Tree parse = (Tree) lp.apply("I have got to go to bed.");
         * parse.pennPrint();
         * 
         * 
         * TreebankLanguagePack tlp = new PennTreebankLanguagePack();
         * GrammaticalStructureFactory gsf = tlp.grammaticalStructureFactory();
         * GrammaticalStructure gs = gsf.newGrammaticalStructure(parse);
         * Collection tdl = gs.typedDependenciesCollapsed();
         * System.out.println(tdl); System.out.println();
         * 
         * 
         * TreePrint tp = new
         * TreePrint("wordsAndTags,penn,typedDependenciesCollapsed");
         * tp.printTree(parse);
         */

        /*
         * lp.parse("Hello my name ist Martin"); Tree tr =
         * lp.getBestPCFGParse(); tr.toString();
         */
        // CollectionHelper.print(Tokenizer.calculateWordNGrams("Hello how are you",2));

    }

    public WhereClassifier getWhereClassifier() {
        return whereClassifier;
    }

    public void setWhereClassifier(int type) {
        this.whereClassifier = new WhereClassifier(type);
        this.whereClassifier
                .useTrainedClassifier("data/learnedClassifiers/where.model");
    }

    public WhoClassifier getWhoClassifier() {
        return whoClassifier;
    }

    public void setWhoClassifier(int type) {
        this.whoClassifier = new WhoClassifier(type);
        this.whoClassifier
                .useTrainedClassifier("data/learnedClassifiers/who.model");
    }

    private static void evaluateEvents() {
        Map<Integer, String[]> events = EventFeatureExtractor
                .readCSV("data/news_articles.csv");
        EventExtractor eventExtractor = EventExtractor.getInstance();
        eventExtractor.setWhoClassifier(Classifier.LINEAR_REGRESSION);
        // eventExtractor.setWhereClassifier(Classifier.LINEAR_REGRESSION);

        for (Entry<Integer, String[]> entry : events.entrySet()) {
            String[] fields = entry.getValue();
            // int id = entry.getKey();

            String url = fields[0];

            String title = fields[1];
            String who = fields[2];
            String where = fields[3];
            String what = fields[4];
            String why = fields[5];
            String how = fields[6];

            Event event = EventExtractor.extractEventFromURL(url);
            EventFeatureExtractor.setFeatures(event);
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

        EventExtractor eventExtractor = EventExtractor.getInstance();
        eventExtractor.setWhoClassifier(Classifier.LINEAR_REGRESSION);
        // eventExtractor.setWhereClassifier(Classifier.LINEAR_REGRESSION);

        Event event = EventExtractor
                .extractEventFromURL("http://www.bbc.co.uk/news/world-europe-11539758");
        EventFeatureExtractor.setFeatures(event);

        StopWatch sw = new StopWatch();
        sw.start();

        // evaluateEvents();
        eventExtractor.extractWho(event);

        sw.stop();
        LOGGER.info("time elapsed: " + sw.getElapsedTimeString());

        /*
         * DatabaseReader reader = new FNDatabaseReader(new File(fnhome),
         * false); reader.read(frameNet);
         */

    }
}
