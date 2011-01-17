package tud.iir.extraction.event;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import tud.iir.classification.FeatureObject;
import tud.iir.extraction.entity.ner.Annotation;
import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Entity;

/**
 * EventFeatureExtractor to extract Features from Events.
 *
 * @author Martin Wunderwald
 */
public class EventFeatureExtractor {

    /** the logger for this class. */
    private static final Logger LOGGER = Logger
            .getLogger(EventFeatureExtractor.class);

    /** NER category mapping person. **/
    public static final double CATEGORY_PERSON = 1.0;
    /** NER category mapping location. **/
    public static final double CATEGORY_LOCATION = 2.0;
    /** NER category mapping org. **/
    public static final double CATEGORY_ORG = 3.0;
    /** NER category mapping noun. **/
    public static final double CATEGORY_NOUN = 4.0;

    /**
     * Constructor.
     */
    public EventFeatureExtractor() {

        // ner.loadModel("data/models/opennlp/namefind/en-ner-person.bin,data/models/opennlp/namefind/en-ner-location.bin,data/models/opennlp/namefind/en-ner-organization.bin");

    }

    /**
     * calculates the features for a set of coreferenced entities within an
     * event.
     *
     * @param event
     */
    public void calculateFeatures(Event event) {
        final HashMap<Annotations, FeatureObject> annotationFeatures = new HashMap<Annotations, FeatureObject>();

        // and finally calculating features for each set of annotations

        for (final Entry<Integer, Annotations> entry : event
                .getCorefAnnotations().entrySet()) {
            annotationFeatures.put(entry.getValue(),
                    calculateAnnotationFeatures(event, entry.getValue()));
        }

        // setting entity features for the chunks
        event.setAnnotationFeatures(annotationFeatures);
    }

    /**
     * Sets the entityFeatures for a whole Map of Events with already
     * coreferenced annotations.
     *
     * @param eventMap
     */
    public void setAnnotationFeatures(Map<String, Event> eventMap) {

        for (final Entry<String, Event> entry : eventMap.entrySet()) {
            final Event event = entry.getValue();
            if (event != null && event.getText() != null) {
                calculateFeatures(event);
            }
        }

    }

    /**
     * calculates features for a Set of Annotations.
     *
     * @param event
     * @param annotations
     * @return the feature Object
     */
    public FeatureObject calculateAnnotationFeatures(Event event,
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

        featureMap.put("titleEntityCount", titleEntityCount);
        featureMap.put("textEntityCount", textEntityCount);
        featureMap.put("type", typeId);
        // featureMap.put("distribution", -Double.valueOf(twoDForm
        // .format(distribution)));
        featureMap.put("distribution", -MathHelper.round(distribution, 3));

        return new FeatureObject(featureMap);

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
     * aggregates events from SearchEngines by a given query.
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
                        calculateFeatures(event);
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
        // tud.iir.extraction.entity.ner.tagger.StanfordNER oner = new
        // tud.iir.extraction.entity.ner.tagger.StanfordNER();
        // oner
        // .loadModel("data/models/stanfordner/data/ner-eng-ie.crf-3-all2008.ser.gz");

        // tud.iir.extraction.entity.ner.tagger.OpenNLPNER oner = new
        // tud.iir.extraction.entity.ner.tagger.OpenNLPNER();
        // oner.loadModel("data/models/opennlp/namefind/en-ner-person.bin,data/models/opennlp/namefind/en-ner-location.bin,data/models/opennlp/namefind/en-ner-organization.bin");

        // tud.iir.extraction.entity.ner.tagger.LingPipeNER oner = new
        // tud.iir.extraction.entity.ner.tagger.LingPipeNER();
        // oner.loadModel("data/models/lingpipe/ne-en-news-muc6.AbstractCharLmRescoringChunker");

        // tud.iir.extraction.entity.ner.tagger.TUDNER oner = new
        // tud.iir.extraction.entity.ner.tagger.TUDNER();
        // oner.loadModel("data/models/tudner/tudner.model");

        final tud.iir.extraction.entity.ner.tagger.OpenCalaisNER oner = new tud.iir.extraction.entity.ner.tagger.OpenCalaisNER();

        final String text = "Pakistan is on high alert as the funeral of assassinated Punjab governor Salman Taseer takes place in Lahore.";
        LOGGER.info(text.length());

        final StopWatch sw = new StopWatch();
        sw.start();

        LOGGER.info(oner.getAnnotations(text).toString());

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
