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

    /** the logger for this class */
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

        final String text = "5 January 2011 Last updated at 09:59 GMT Pakistan is on high alert as the funeral of assassinated Punjab governor Salman Taseer takes place in Lahore. Mr Taseer was shot dead by one of his own bodyguards who was angered by his opposition to blasphemy laws. Thousands gathered in Lahore for the burial of Mr Taseer, one of Pakistan's most outspoken liberal politicians. US Secretary of State Hillary Clinton described his death as \"a great loss\", saying he had promoted tolerance. The governor - a senior member of the governing Pakistan People's Party (PPP) - had recently angered Islamists by appealing for a Christian woman, sentenced to death for blasphemy, to be pardoned. Acting alone? Prime Minister Yousuf Raza Gilani declared three days of national mourning and appealed for calm. However, some religious leaders have praised the governor's killer and called for a boycott of the ceremonies in Lahore, says the BBC's Orla Guerin in Islamabad. Continue reading the main story Analysis M Ilyas Khan BBC News, Islamabad The assassination of Governor Salman Taseer appears to have raised the level of threat against liberal voices in Pakistan. While many religious leaders have publicly justified the murder, the liberal sections of the society have been more cautious in condemning it. This is due to the rising tendency in the society to silence voices of religious dissent by force, a tendency promoted by militant groups and condoned by religious forces active in the political sphere. Even within the clerical community, many liberal voices have been silenced. Some have been blown up in suicide attacks, others have migrated. In a country where religious politicians have never won an election, this policy of intimidation has expanded their influence. They often distance themselves from acts of militancy but never fail to justify them. For example, they often condemn suicide attacks by militants on civilian targets, but in the same breath qualify the act as caused by 'anger over excesses being committed against Muslims by Western powers'. Following Mr Taseer's assassination, they mostly did the same: condemning the act but justifying the killer who 'acted in defence of the dignity of the Prophet'. As is evident from Mr Taseer's assassination, any counter-argument can invoke a decree of death. Five hundred scholars from the moderate Barelvi sect of Sunni Muslims have warned that anyone who expresses grief over the assassination could suffer the same fate. \"No Muslim should attend the funeral or even try to pray for Salman Taseer or even express any kind of regret or sympathy over the incident,\" said the Jamaat-e-Ahl-e-Sunnat Pakistan in a statement. It said anyone who expressed sympathy over the death of a blasphemer was also committing blasphemy. One of Mr Taseer's bodyguards, Malik Mumtaz Hussein Qadri, was detained immediately after the shooting at Kohsar Market in Islamabad. He confessed to the murder, said Pakistan's interior minister, Rehman Malik. Police are now questioning the rest of Mr Taseer's security detail and are also carrying out an inquiry into the governor's security arrangements. \"We will investigate whether it was an individual act or there is some organisation behind it,\" Mr Malik told a news conference. The BBC's Ilyas Khan says the most obvious questions being asked at the moment are whether the killer acted alone, and why did other members of Mr Taseer's security team not try to prevent the assassination. There are few credible explanations so far as to why the guard was able to empty two magazines of his sub-machine gun on the governor without being shot by his colleagues, our correspondent says. 'A great loss' The assassination was condemned by world leaders. \"I had the opportunity to meet Governor Taseer in Pakistan and I admired his work to promote tolerance and the education of Pakistan's future generations,\" said Mrs Clinton in a statement. \"His death is a great loss.\" UN Secretary General Ban Ki-moon and UK Foreign Secretary William Hague also spoke out against the killing. Pakistan's high commissioner to London, Wajid Shamshul Hassan, told the BBC's Newshour programme that the assassination exposed the divisions in his country. \"It has shown that you can be held hostage by a minority of [radical] religious people and they can do whatever they want. That is not the way we are going to allow in the country,\" he said. \"We will be tough on them. Unless we get rid of such people in our society, unless we purge them from the various security agencies, you can't feel that justice will be done.\" Mr Taseer - a close associate of President Asif Ali Zardari - made headlines by appealing for the pardon of Christian woman Asia Bibi who had been sentenced to death for allegedly insulting the Prophet Muhammad. His death is the most high-profile assassination in Pakistan since former prime minister Benazir Bhutto was killed in December 2007. The BBC's Aleem Maqbool in Islamabad says Mr Taseer was one of Pakistan's most important political figures and his death will add further instability to the country. The government led by Mr Taseer's Pakistan People's Party is under threat after one of its coalition partners walked out at the weekend. The International Monetary Fund (IMF) is withholding the latest tranche of its $11.3bn loan to Islamabad, while petrol prices have increased sharply and chronic fuel shortages are causing unrest. Pakistan is also under pressure from the US to move against militants in the tribal areas bordering Afghanistan.";

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
