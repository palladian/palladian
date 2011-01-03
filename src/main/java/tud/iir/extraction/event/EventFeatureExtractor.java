package tud.iir.extraction.event;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.classification.FeatureObject;
import tud.iir.extraction.entity.ner.Annotation;
import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.NamedEntityRecognizer;
import tud.iir.extraction.entity.ner.tagger.LingPipeNER;
import tud.iir.extraction.entity.ner.tagger.OpenNLPNER;
import tud.iir.helper.ConfigHolder;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Entity;

import com.aliasi.coref.EnglishMentionFactory;
import com.aliasi.coref.Mention;
import com.aliasi.coref.MentionFactory;
import com.aliasi.coref.WithinDocCoref;

/**
 * EventFeatureExtractor to extract Features from Events
 * 
 * @author Martin Wunderwald
 */
public class EventFeatureExtractor {

    /** the logger for this class */
    private static final Logger LOGGER = Logger
    .getLogger(EventFeatureExtractor.class);

    /** Instance of this EventFeatureExtractor. **/
    private static EventFeatureExtractor instance = null;

    /** model file for lingpipe rescoring chunker from muc6. */
    private final String MODEL_NER;

    public static final double CATEGORY_PERSON = 1.0;
    public static final double CATEGORY_LOCATION = 2.0;
    public static final double CATEGORY_ORG = 3.0;
    public static final double CATEGORY_NOUN = 4.0;

    /** The NamedEntityRecognizer. **/
    private static NamedEntityRecognizer ner = new LingPipeNER();

    /** The POS-Tagger used in this class **/
    private static AbstractPOSTagger posTagger = new OpenNLPPOSTagger();

    /** The PhraseChunker. **/
    private static AbstractPhraseChunker phraseChunker = new OpenNLPPhraseChunker();

    /** The Parser. **/
    private static AbstractParser parser = new OpenNLPParser();

    /** The SentenceDetector. **/
    private static AbstractSentenceDetector sentenceDetector = new OpenNLPSentenceDetector();

    protected EventFeatureExtractor() {

        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            MODEL_NER = config.getString("models.lingpipe.en.ner");
        } else {
            MODEL_NER = "";
        }
        // ner.loadModel("data/models/opennlp/namefind/en-ner-person.bin,data/models/opennlp/namefind/en-ner-location.bin,data/models/opennlp/namefind/en-ner-organization.bin");
        ner.loadModel(MODEL_NER);
        posTagger.loadModel();
        phraseChunker.loadModel();
        parser.loadModel();
    }

    public static EventFeatureExtractor getInstance() {
        if (instance == null) {
            instance = new EventFeatureExtractor();
        }
        return instance;
    }

    /**
     * sets the features of an event
     * 
     * @param event
     */
    public void setFeatures(Event event) {
        event.setSentences(getSentences(event.getText()));
        setAnnotations(event);
        setAnnotationFeatures(event);
    }

    /**
     * sets the entityFeatures for a whole Map of Events
     * 
     * @param eventMap
     */
    public void setAnnotationFeatures(Map<String, Event> eventMap) {

        for (final Entry<String, Event> entry : eventMap.entrySet()) {
            final Event event = entry.getValue();
            if (event != null && event.getText() != null) {
                setAnnotations(event);
                setAnnotationFeatures(event);
            }
        }

    }

    /**
     * sets the EntityFeatures for a given event
     * 
     * @param event
     */
    public void setAnnotationFeatures(Event event) {

        final HashMap<Integer, Annotations> corefAnnotations = (HashMap<Integer, Annotations>) getCoreferenceAnnotations(event);
        final HashMap<Annotations, FeatureObject> annotationFeatures = new HashMap<Annotations, FeatureObject>();

        // setting coreferenceChunkSet

        for (final Entry<Integer, Annotations> entry : corefAnnotations
                .entrySet()) {
            annotationFeatures.put(entry.getValue(),
                    calculateAnnotationFeatures(event, entry.getValue()));
        }

        // setting entity features for the chunks
        event.setAnnotationFeatures(annotationFeatures);

    }

    /**
     * performing co-reference resolution
     * 
     * @param event
     * @return
     */
    private Map<Integer, Annotations> getCoreferenceAnnotations(Event event) {

        LOGGER.info("performing coreference: " + event.getTitle());
        /*
         * final StopWatch stopWatch = new StopWatch(); stopWatch.start();
         */
        final MentionFactory mfactory = new EnglishMentionFactory();
        final WithinDocCoref coref = new WithinDocCoref(mfactory);

        final Annotations annotations = event.getAnnotations();
        final HashMap<Integer, Annotations> corefAnnotationMap = new HashMap<Integer, Annotations>();

        int mentionId;
        Mention mention;
        String phrase;
        Annotations tmpAnnotations;
        final Iterator<Annotation> it = annotations.iterator();

        while (it.hasNext()) {

            final Annotation annotation = it.next();

            phrase = annotation.getEntity().getName();

            mention = mfactory
            .create(phrase, annotation.getMostLikelyTagName());
            mentionId = coref.resolveMention(mention, 1);

            tmpAnnotations = new Annotations();

            if (corefAnnotationMap.containsKey(mentionId)) {
                tmpAnnotations = corefAnnotationMap.get(mentionId);
                tmpAnnotations.add(annotation);
                corefAnnotationMap.put(mentionId, tmpAnnotations);

            } else {
                tmpAnnotations.add(annotation);
                corefAnnotationMap.put(mentionId, tmpAnnotations);

            }
        }
        /*
         * stopWatch.stop(); LOGGER.info("NER + coreference Resolution took: " +
         * stopWatch.getElapsedTime());
         */
        return corefAnnotationMap;

    }

    /**
     * calculates features for a Set of Chunks
     * 
     * @param event
     * @param chunkSet
     * @return
     */
    private static FeatureObject calculateAnnotationFeatures(Event event,
            Annotations annotations) {

        final HashMap<String, Double> featureMap = new HashMap<String, Double>();

        double textEntityCount = 0.0;
        double titleEntityCount = 0.0;
        double avgOffset = 0.0;
        double typeId = 0.0;

        String phrase;

        for (final Annotation annotation : annotations) {
            phrase = annotation.getEntity().getName();

            if (phrase.length() > 3) {

                textEntityCount += 1.0;

                if (titleEntityCount == 0.0) {
                    titleEntityCount = countEntityOccurrences(annotation
                            .getEntity(), event.getTitle());
                }

                if (annotation.getMostLikelyTagName().equals("PERSON")) {
                    typeId = CATEGORY_PERSON;
                }
                if (annotation.getMostLikelyTagName().equals("ORGANIZATION")) {
                    typeId = CATEGORY_ORG;
                }
                if (annotation.getMostLikelyTagName().equals("LOCATION")) {
                    typeId = CATEGORY_LOCATION;
                }
                if (annotation.getMostLikelyTagName().equals("NOUNPHRASE")) {
                    typeId = CATEGORY_NOUN;
                }

                avgOffset += annotation.getOffset();

            }

        }

        final double distribution = avgOffset / annotations.size()
        / event.getText().length();

        final DecimalFormat twoDForm = new DecimalFormat("#.###");

        featureMap.put("titleEntityCount", titleEntityCount);
        featureMap.put("textEntityCount", textEntityCount);
        featureMap.put("type", typeId);
        // featureMap.put("distribution", -Double.valueOf(twoDForm
        // .format(distribution)));
        featureMap.put("distribution", -MathHelper.round(distribution, 3));

        return new FeatureObject(featureMap);

    }

    /**
     * Annotates NounPhrases in Title and first Sentence.
     * 
     * @param event
     * @return
     */
    private Annotations getNounAnnotations(String text) {

        phraseChunker.loadModel();
        phraseChunker.chunk(text);

        final TagAnnotations tagAnnotations = phraseChunker.getTagAnnotations();
        final Annotations annotations = new Annotations();

        for (final TagAnnotation tagAnnotation : tagAnnotations) {
            if (tagAnnotation.getTag().equals("NP")) {
                final Annotation annotation = new Annotation(tagAnnotation
                        .getOffset(), tagAnnotation.getChunk(), "NOUNPHRASE");
                annotations.add(annotation);
            }

        }

        return annotations;

    }

    public Annotations getDateAnnotations(String text) {
        OpenNLPNER timeNer = new OpenNLPNER();
        timeNer
        .loadModel("data/models/opennlp/namefind/en-ner-time.bin,data/models/opennlp/namefind/en-ner-date.bin");

        return timeNer.getAnnotations(text);

    }

    /**
     * performs Namend Entity Recognition on the given event and annotates nouns
     * in headline
     * 
     * @param event
     * @return
     */
    private void setAnnotations(Event event) {

        // NamedEntityRecognizer ner = new LingPipeNER();
        // NamedEntityRecognizer ner = new IllinoisLbjNER();
        // NamedEntityRecognizer ner = new AlchemyNER();
        // NamedEntityRecognizer ner = new OpenCalaisNER();

        // NamedEntityRecognizer ner = new TUDNER();
        final Annotations annotations = new Annotations();
        final Annotations textAnnotations = ner.getAnnotations(event.getText());
        final Annotations titleAnnotations = ner.getAnnotations(event
                .getTitle());
        final Annotations nounAnnotations = getNounAnnotations(event.getTitle());

        // removing duplicate title annotations
        for (final Annotation annotation : nounAnnotations) {
            for (final Annotation anno : titleAnnotations) {
                if (!annotation.overlaps(anno)
                        && !annotations.contains(annotation)) {
                    annotations.add(annotation);
                }
            }
        }

        annotations.addAll(textAnnotations);

        event.setAnnotations(annotations);

    }

    public TagAnnotations getPOSTags(String sentence) {
        posTagger.tag(sentence);
        return posTagger.getTagAnnotations();
    }

    /**
     * performs phrase chunking on a sentence
     * 
     * @param sentence
     *            - The sentence
     * @return The part of speach tags.
     */
    public TagAnnotations getPhraseChunks(String sentence) {
        phraseChunker.chunk(sentence);
        return phraseChunker.getTagAnnotations();
    }

    /**
     * returns a Parse on a sentence.
     * 
     * @param sentence
     * @return the parse
     */
    public TagAnnotations getParse(String sentence) {
        parser.loadModel();
        parser.parse(sentence);
        return parser.getTagAnnotations();
    }

    /**
     * Split a provided string into sentences and return a set of sentence
     * chunks.
     * 
     * @param sentence
     * @return
     */
    public String[] getSentences(String text) {
        sentenceDetector.loadModel();
        sentenceDetector.detect(text);
        return sentenceDetector.getSentences();
    }

    /**
     * counts Entity occurences with the help of dictonaryChunker
     * 
     * @param entity
     *            - the entity
     * @param article
     *            - the article
     * @param includePrefixes
     * @return count of occurrences
     */
    public static int countEntityOccurrences(Entity entity, String article) {

        return StringHelper.countOccurences(article, StringHelper
                .escapeForRegularExpression(entity.getName()), true);

    }

    /**
     * aggregates events from SearchEngines by a given query
     * 
     * @param query
     *            - the query
     * @return eventMap
     */
    public static Map<String, Event> aggregateEvents(final String query) {

        final EventAggregator aggregator = new EventAggregator();
        // ea.setSearchEngine(SourceRetrieverManager.GOOGLE_NEWS);
        aggregator.setMaxThreads(5);
        aggregator.setResultCount(15);
        aggregator.setQuery(query);
        aggregator.aggregate();

        return aggregator.getEventmap();
    }

    /**
     * reads an Map of events from csv file.
     * 
     * @param filePath
     * @return
     */
    public Map<Integer, String[]> readCSV(String filePath) {

        FileReader csvFileRead;

        final Map<Integer, String[]> events = new HashMap<Integer, String[]>();

        try {
            csvFileRead = new FileReader(filePath);
            final BufferedReader csvFile = new BufferedReader(csvFileRead);
            String csvFileLine = "";
            int csvFileLineNumber = 1;
            while ((csvFileLine = csvFile.readLine()) != null) {
                events.put(csvFileLineNumber, csvFileLine.split(";"));
                csvFileLineNumber++;
            }

            csvFile.close();

        } catch (final FileNotFoundException e) {
            LOGGER.error(e);
        } catch (final IOException e) {
            LOGGER.error(e);
        }

        return events;
    }

    /**
     * writes events to CSV file for training the classifier.
     * 
     * @param eventMap
     * @param whos
     * @param wheres
     * @param whats
     * @param append
     */
    public void writeCSV(String outFilePath, Map<String, Event> eventMap,
            List<String> positives, boolean append) {

        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(outFilePath, append);

            final String separator = ";";

            /*
             * if (!append) { fileWriter.write("\"titleEntityCount\"" +
             * separator + "\"density\"" + separator + "\"textEntityCount\"" +
             * separator + "\"avgStart\"" + separator + "\"type\"" + separator +
             * "\"avgEnd\"" + separator + "\"class\"" + separator + "\n");
             * fileWriter.flush(); }
             */

            for (final Entry<String, Event> eentry : eventMap.entrySet()) {
                final Event event = eentry.getValue();
                if (event != null && event.getText() != null) {

                    Map<Annotations, FeatureObject> featureMap = event
                    .getAnnotationFeatures();
                    // hm.put(url, e);

                    if (event.getAnnotationFeatures() == null) {
                        setFeatures(event);
                        featureMap = event.getAnnotationFeatures();
                    }

                    for (final Entry<Annotations, FeatureObject> eeentry : featureMap
                            .entrySet()) {

                        final FeatureObject features = eeentry.getValue();
                        final Annotations annotations = eeentry.getKey();

                        // only for where classifier
                        if (features.getFeature("type").equals(
                                EventFeatureExtractor.CATEGORY_LOCATION)) {

                            // fileWriter.write(id + separator);
                            // final StringBuffer text = new StringBuffer();
                            boolean contains = false;

                            for (final Annotation annotation : annotations) {

                                contains = false;
                                for (final String positive : positives) {

                                    if (annotation.getEntity().getName()
                                            .toLowerCase().contains(
                                                    positive.toLowerCase())
                                                    || positive.contains(annotation
                                                            .getEntity().getName()
                                                            .toLowerCase())) {
                                        contains = true;
                                    }

                                }
                                if (contains) {
                                    features.setClassAssociation(2);
                                }

                                /*
                                 * if (whos.contains(annotation.getEntity()
                                 * .getName())) { fo.setClassAssociation(1); }
                                 * if (whats.contains(annotation.getEntity()
                                 * .getName())) { fo.setClassAssociation(3); }
                                 */
                                // text.append(annotation.getEntity().getName());
                            }

                            for (final Double d : features.getFeatures()) {
                                fileWriter.write(d.toString() + separator);
                            }
                            if (features.getClassAssociation() == 2) {
                                fileWriter.write("1.0");
                            }

                            else {
                                fileWriter.write("0.0");
                            }
                            fileWriter.write("\n");

                            fileWriter.flush();
                        }
                    }

                }

            }

            // fileWriter.write("\n");
            fileWriter.flush();
            fileWriter.close();

        } catch (final IOException ex) {
            LOGGER.error(ex);
        }
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // String sentence = "NBC Universal drops ad partnership with Google";

        // final Event event1 = EventExtractor
        // .extractEventFromURL("http://articles.latimes.com/2010/oct/14/business/la-fi-ct-nbcgoogle-20101014");
        //

        // CollectionHelper.print(getNounAnnotations("this is my sentence"));
        /*
         * posTagger.loadModel(); posTagger.tag(event1.getTitle());
         * CollectionHelper.print(posTagger.getTagAnnotations());
         * phraseChunker.loadModel(); phraseChunker.chunk(event1.getTitle());
         * CollectionHelper.print(phraseChunker.getTagAnnotations());
         * parser.loadModel(); parser.parse(event1.getTitle());
         * CollectionHelper.print(parser.getTagAnnotations());
         */

        // EventFeatureExtractor featureExtractor = new EventFeatureExtractor();
        // LOGGER.setLevel(Level.ALL);
        // posTagger.tag(event1.getTitle());
        // LOGGER.info(posTagger.getTagAnnotations().getTaggedString());

        // phraseChunker.chunk(event1.getTitle());
        // LOGGER.info(phraseChunker.getTagAnnotations().getTaggedString());
        /*
         * parser.loadModel(); for (String str :
         * featureExtractor.getSentences(event1.getText())) { ((OpenNLPParser)
         * parser).link(((OpenNLPParser) parser) .getFullParse(str)); }
         */
        // ner = new OpenNLPNER();
        // ner.loadModel("data/models/opennlp/namefind/en-ner-person.bin,data/models/opennlp/namefind/en-ner-location.bin,data/models/opennlp/namefind/en-ner-organization.bin");

        final StopWatch sw = new StopWatch();
        sw.start();

        // PhraseChunker pc = new PhraseChunker(PhraseChunker.CHUNKER_OPENNLP);
        // pc.chunk(event.getTitle());

        // setFeatures(event);

        // Parser.openNLPParse("this is my sentence");

        // CollectionHelper.print(pc.getTags());
        // CollectionHelper.print(pc.getTokens());
        // setFeatures(event);

        sw.stop();
        LOGGER.info("time elapsed: " + sw.getElapsedTimeString());

    }
}
