package tud.iir.extraction.event;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;

import tud.iir.classification.FeatureObject;
import tud.iir.extraction.entity.ner.Annotation;
import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.NamedEntityRecognizer;
import tud.iir.extraction.entity.ner.tagger.LingPipeNER;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Entity;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.ChunkFactory;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
import com.aliasi.coref.EnglishMentionFactory;
import com.aliasi.coref.Mention;
import com.aliasi.coref.MentionFactory;
import com.aliasi.coref.WithinDocCoref;
import com.aliasi.dict.ApproxDictionaryChunker;
import com.aliasi.dict.DictionaryEntry;
import com.aliasi.dict.TrieDictionary;
import com.aliasi.sentences.IndoEuropeanSentenceModel;
import com.aliasi.sentences.SentenceChunker;
import com.aliasi.sentences.SentenceModel;
import com.aliasi.spell.FixedWeightEditDistance;
import com.aliasi.spell.WeightedEditDistance;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * EventFeatureExtractor to extract Features from Events
 * 
 * @author Martin Wunderwald
 */
public class EventFeatureExtractor {

    private static EventFeatureExtractor instance = null;

    private static final String MODEL_PATH = "data/models/";

    /** model file for lingpipe rescoring chunker from muc6 */
    private static final String MODEL_NER_LINGPIPE = MODEL_PATH
            + "lingpipe/ne-en-news-muc6.AbstractCharLmRescoringChunker";

    private static final double CATEGORY_PERSON = 1.0;
    private static final double CATEGORY_LOCATION = 2.0;
    private static final double CATEGORY_ORGANIZATION = 3.0;
    private static final double CATEGORY_NOUNPHRASE = 4.0;

    private static NamedEntityRecognizer ner = new LingPipeNER();
    private static AbstractPOSTagger posTagger = new OpenNLPPOSTagger();
    private static AbstractPhraseChunker phraseChunker = new OpenNLPPhraseChunker();

    /** the logger for this class */
    private static final Logger LOGGER = Logger.getLogger(EventExtractor.class);

    protected EventFeatureExtractor() {

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
    public static void setFeatures(Event event) {
        setEntityFeatures(event);
    }

    /**
     * sets the entityFeatures for a whole Map of Events
     * 
     * @param eventMap
     */
    public static void setEntityFeatures(Map<String, Event> eventMap) {

        for (Entry<String, Event> entry : eventMap.entrySet()) {
            final Event event = entry.getValue();
            if (event != null && event.getText() != null) {
                setEntityFeatures(event);
            }
        }

    }

    /**
     * sets the EntityFeatures for a given event
     * 
     * @param event
     */
    private static void setEntityFeatures(Event event) {

        final HashMap<Integer, Annotations> corefAnnotations = (HashMap<Integer, Annotations>) getCoreferenceAnnotations(event);
        final HashMap<Integer, FeatureObject> featureObjects = new HashMap<Integer, FeatureObject>();

        // setting coreferenceChunkSet
        event.setEntityAnnotations(corefAnnotations);

        for (Entry<Integer, Annotations> entry : corefAnnotations.entrySet()) {
            featureObjects.put(entry.getKey(),
                    calculateEntityAnnotationFeatures(event, entry.getValue()));
        }

        // setting entity features for the chunks
        event.setEntityFeatures(featureObjects);

    }

    /**
     * performing co-reference resolution
     * 
     * @param event
     * @return
     */
    private static Map<Integer, Annotations> getCoreferenceAnnotations(
            Event event) {

        LOGGER.info("performing coreference: " + event.getTitle());

        final MentionFactory mfactory = new EnglishMentionFactory();
        final WithinDocCoref coref = new WithinDocCoref(mfactory);

        final Annotations annotations = getEntityAnnotations(event);
        final HashMap<Integer, Annotations> corefAnnotationMap = new HashMap<Integer, Annotations>();

        int mentionId;
        Mention mention;
        String phrase;
        Annotations tmpAnnotations;
        Iterator<Annotation> it = annotations.iterator();

        while (it.hasNext()) {

            Annotation annotation = it.next();

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

        return corefAnnotationMap;

    }

    /**
     * calculates features for a Set of Chunks
     * 
     * @param event
     * @param chunkSet
     * @return
     */
    private static FeatureObject calculateEntityAnnotationFeatures(Event event,
            Annotations annotations) {

        final HashMap<String, Double> featureMap = new HashMap<String, Double>();

        double textEntityCount = 0.0;
        double titleEntityCount = 0.0;
        double avgOffset = 0.0;
        double typeId = 0.0;

        String phrase;

        for (Annotation annotation : annotations) {
            phrase = annotation.getEntity().getName();

            if (phrase.length() > 3) {

                textEntityCount += 1.0;

                if (titleEntityCount == 0.0) {
                    titleEntityCount = (double) countEntityOccurrences(
                            annotation.getEntity(), event.getTitle());
                }

                if (annotation.getMostLikelyTagName().equals("PERSON")) {
                    typeId = CATEGORY_PERSON;
                }
                if (annotation.getMostLikelyTagName().equals("ORGANIZATION")) {
                    typeId = CATEGORY_ORGANIZATION;
                }
                if (annotation.getMostLikelyTagName().equals("LOCATION")) {
                    typeId = CATEGORY_LOCATION;
                }
                if (annotation.getMostLikelyTagName().equals("NOUNPHRASE")) {
                    typeId = CATEGORY_NOUNPHRASE;
                }

                avgOffset += annotation.getOffset();

            }

        }

        double distribution = (avgOffset / annotations.size())
                / event.getText().length();

        DecimalFormat twoDForm = new DecimalFormat("#.###");

        featureMap.put("titleEntityCount", titleEntityCount);
        featureMap.put("textEntityCount", textEntityCount);
        featureMap.put("type", typeId);
        featureMap.put("distribution", Double.valueOf(twoDForm
                .format(distribution)));

        return new FeatureObject(featureMap);

    }

    /**
     * Annotates NounPhrases in Title and first Sentence
     * 
     * @param event
     * @return
     */
    private static Annotations getNounAnnotations(String text) {

        Annotations annotations = new Annotations();

        phraseChunker.loadModel();
        phraseChunker.chunk(text);

        List<String> phrases = phraseChunker.getChunks();
        List<String> tokens = phraseChunker.getTokens();

        int offset = 0;
        for (int i = 0; i < phrases.size(); i++) {

            if (phrases.get(i).equals("NP")) {
                Annotation annotation = new Annotation(offset, tokens.get(i),
                        "NOUNPHRASE");
                annotations.add(annotation);
            }
            offset += tokens.get(i).length() + 1;

        }

        return annotations;

    }

    /**
     * performs Namend Entity Recognition on the given event
     * 
     * @param event
     * @return
     */
    private static Annotations getEntityAnnotations(Event event) {

        // NamedEntityRecognizer ner = new LingPipeNER();
        // NamedEntityRecognizer ner = new IllinoisLbjNER();
        // NamedEntityRecognizer ner = new AlchemyNER();
        // NamedEntityRecognizer ner = new OpenCalaisNER();
        if (ner == null) {
            ner = new LingPipeNER();
        }

        if (ner.getModel() == null) {
            // ner.loadModel("data/models/ner-eng-ie.crf-3-all2008.ser.gz");
            ner.loadModel(MODEL_NER_LINGPIPE);
        }

        // NamedEntityRecognizer ner = new TUDNER();
        Annotations annotations = new Annotations();
        Annotations textAnnotations = ner.getAnnotations(event.getText());
        Annotations titleAnnotations = ner.getAnnotations(event.getTitle());
        Annotations nounAnnotations = getNounAnnotations(event.getTitle());

        // removing duplicate title annotations
        for (Annotation annotation : nounAnnotations) {
            for (Annotation anno : titleAnnotations) {
                if (!annotation.overlaps(anno)
                        && !annotations.contains(annotation)) {
                    annotations.add(annotation);
                }
            }

        }

        annotations.addAll(textAnnotations);

        return annotations;
    }

    /**
     * Extract a list of part-of-speech tags from a sentence.
     * 
     * @param sentence
     *            - The sentence
     * @return The part of speach tags.
     */
    public static void getPhraseChunks(String sentence) {

    }

    /**
     * Return the set of occurrences of a certain entity in a provided string,
     * including different spellings of the entity.
     * 
     * An optional parameter allows to specify whether the entity might be
     * prefixed by "the", "an" or "a".
     * 
     * @param entity
     * @param text
     * @param includePrefixes
     * @return
     */
    public static Set<Chunk> getDictionaryChunksForEntity(Entity entity,
            String text, boolean includePrefixes) {

        // lowercase everything

        String entityName = entity.getName().toLowerCase();

        ArrayList<String> prefixes = new ArrayList<String>();
        prefixes.add("the");
        prefixes.add("an");
        prefixes.add("a");

        ArrayList<String> synonyms = new ArrayList<String>();
        // synonyms.add(entity.getName().toLowerCase());

        // Approximate Dictionary-Based Chunking

        double maxDistance = 2.0;

        TrieDictionary<String> dict = new TrieDictionary<String>();

        // matches
        dict
                .addEntry(new DictionaryEntry<String>(entityName, entity
                        .getName()));
        if (includePrefixes) {
            for (String prefix : prefixes) {
                DictionaryEntry<String> dictEntry = new DictionaryEntry<String>(
                        prefix + " " + entityName, entity.getName());
                dict.addEntry(dictEntry);
            }
        }

        // synonyms
        for (String synonym : synonyms) {

            DictionaryEntry<String> dictEntry = new DictionaryEntry<String>(
                    synonym, entity.getName());

            dict.addEntry(dictEntry);

            if (includePrefixes) {
                for (String prefix : prefixes) {
                    DictionaryEntry<String> dEntry = new DictionaryEntry<String>(
                            prefix + " " + synonym, entity.getName());
                    dict.addEntry(dEntry);
                }
            }
        }

        WeightedEditDistance editDistance = new FixedWeightEditDistance(0, -1,
                -1, -1, Double.NaN);

        Chunker chunker = new ApproxDictionaryChunker(dict,
                IndoEuropeanTokenizerFactory.INSTANCE, editDistance,
                maxDistance);

        return chunker.chunk(text).chunkSet();
    }

    /**
     * Split a provided string into sentences and return a set of sentence
     * chunks.
     */
    public static Set<Chunk> getSentenceChunks(String text) {

        if (text == null) {
            return null;
        }

        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        SentenceModel sentenceModel = new IndoEuropeanSentenceModel();

        SentenceChunker sentenceChunker = new SentenceChunker(tokenizerFactory,
                sentenceModel);
        Chunking chunking = sentenceChunker.chunk(text.toCharArray(), 0, text
                .length());

        return chunking.chunkSet();
    }

    @SuppressWarnings("unused")
    private static String resolveChunkSet(Set<Chunk> chunkSet, Event event) {
        Iterator<Chunk> iterator = chunkSet.iterator();
        StringBuilder sb = new StringBuilder();
        while (iterator.hasNext()) {

            Chunk chunk = iterator.next();

            String phrase = event.getText().substring(chunk.start(),
                    chunk.end()).toLowerCase();

            sb.append(phrase + " ");
        }

        return sb.toString();
    }

    @SuppressWarnings("unused")
    private static double calculateChunkDensity(Chunk chunk,
            Set<Chunk> chunkSet, int window) {

        double density = 0.0;

        for (Chunk cuk : chunkSet) {
            if ((cuk.start() > chunk.start() - window)
                    && (cuk.start() < chunk.start())
                    && cuk.type().equals("LOCATION")) {
                density++;
            }
            if ((cuk.end() < chunk.end() + window) && (cuk.end() > chunk.end())
                    && cuk.type().equals("LOCATION")) {
                density++;
            }
        }

        return density;
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

        // return getDictionaryChunksForEntity(entity, article, false).size();

    }

    /**
     * aggregates events from SearchEngines by a given query
     * 
     * @param query
     *            - the query
     * @return eventMap
     */
    public static Map<String, Event> aggregateEvents(final String query) {

        EventAggregator ea = new EventAggregator();
        // ea.setSearchEngine(SourceRetrieverManager.GOOGLE_NEWS);
        ea.setMaxThreads(5);
        ea.setResultCount(15);
        ea.setQuery(query);
        ea.aggregate();

        return ea.getEventmap();
    }

    /**
     * builds a searchengine query by given triple of whos,wheres,whats
     * 
     * @param whos
     * @param wheres
     * @param whats
     * @return
     */
    @SuppressWarnings("unused")
    private static String buildQuery(List<String> whos, List<String> wheres,
            List<String> whats) {

        StringBuilder sb = new StringBuilder();

        sb.append("news ");

        for (String who : whos) {
            sb.append(who + " ");
        }
        for (String where : wheres) {
            sb.append(where + " ");
        }
        for (String what : whats) {
            sb.append(what + " ");
        }
        return sb.toString();
    }

    /**
     * converts an Annotations to a chunkSet.
     * 
     * @param Annotations
     * @return
     */
    @SuppressWarnings("unused")
    private static Set<Chunk> annotations2chunkSet(Annotations annotations) {

        Set<Chunk> chunkSet = new TreeSet<Chunk>(Chunk.TEXT_ORDER_COMPARATOR);

        for (Annotation annotation : annotations) {

            Chunk chunk = ChunkFactory.createChunk(annotation.getOffset(),
                    annotation.getEndIndex(),
                    annotation.getMostLikelyTagName(),
                    ChunkFactory.DEFAULT_CHUNK_SCORE);
            chunkSet.add(chunk);
        }

        return chunkSet;
    }

    /**
     * converts an Set of Chunks to Annotations.
     * 
     * @param chunkSet
     * @param text
     * @return
     */
    @SuppressWarnings("unused")
    private static Annotations chunkSet2Annotations(Set<Chunk> chunkSet,
            String text) {
        Annotations annotations = new Annotations();

        for (Chunk chunk : chunkSet) {

            Annotation annotation = new Annotation(chunk.start(), text
                    .substring(chunk.start(), chunk.end()), chunk.type());
            annotations.add(annotation);

        }
        return annotations;

    }

    /**
     * reads an Map of events from csv file
     * 
     * @param filePath
     * @return
     */
    public static Map<Integer, String[]> readCSV(String filePath) {

        FileReader csvFileRead;

        Map<Integer, String[]> events = new HashMap<Integer, String[]>();

        try {
            csvFileRead = new FileReader(filePath);
            BufferedReader csvFile = new BufferedReader(csvFileRead);
            String csvFileLine = "";
            int csvFileLineNumber = 1;
            while ((csvFileLine = csvFile.readLine()) != null) {
                events.put(csvFileLineNumber, csvFileLine.split(";"));
                csvFileLineNumber++;
            }

            csvFile.close();

        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        } catch (IOException e) {
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
    public static void writeCSV(String outFilePath,
            Map<String, Event> eventMap, List<String> positives, boolean append) {

        FileWriter fileWriter;
        try {
            fileWriter = new FileWriter(outFilePath, append);

            String separator = ";";

            /*
             * if (!append) { fileWriter.write("\"titleEntityCount\"" +
             * separator + "\"density\"" + separator + "\"textEntityCount\"" +
             * separator + "\"avgStart\"" + separator + "\"type\"" + separator +
             * "\"avgEnd\"" + separator + "\"class\"" + separator + "\n");
             * fileWriter.flush(); }
             */

            for (Entry<String, Event> eentry : eventMap.entrySet()) {
                Event ev = eentry.getValue();
                if (ev != null && ev.getText() != null) {

                    Map<Integer, FeatureObject> featureMap = ev
                            .getEntityFeatures();
                    Map<Integer, Annotations> annotationsMap = ev
                            .getEntityAnnotations();
                    // hm.put(url, e);

                    if (ev.getEntityFeatures() == null) {
                        setFeatures(ev);
                        featureMap = ev.getEntityFeatures();
                        annotationsMap = ev.getEntityAnnotations();
                        // CollectionHelper.print(featureMap);
                    }

                    for (Entry<Integer, FeatureObject> eeentry : featureMap
                            .entrySet()) {

                        FeatureObject fo = eeentry.getValue();
                        Integer id = eeentry.getKey();

                        Annotations annotations = annotationsMap.get(id);

                        // fileWriter.write(id + separator);
                        String text = "";
                        for (Annotation annotation : annotations) {

                            boolean contains = false;
                            for (String positive : positives) {

                                if (annotation.getEntity().getName().contains(
                                        positive)
                                        || positive.contains(annotation
                                                .getEntity().getName())) {
                                    contains = true;
                                }

                            }
                            if (contains) {
                                fo.setClassAssociation(2);
                            }

                            /*
                             * if (whos.contains(annotation.getEntity()
                             * .getName())) { fo.setClassAssociation(1); } if
                             * (whats.contains(annotation.getEntity()
                             * .getName())) { fo.setClassAssociation(3); }
                             */
                            text += annotation.getEntity().getName();
                        }
                        for (Double d : fo.getFeatures()) {
                            fileWriter.write(d.toString() + separator);
                        }
                        if (fo.getClassAssociation() == 2) {
                            fileWriter.write("1.0");
                        }
                        /*
                         * else if (fo.getClassAssociation() == 1) {
                         * 
                         * fileWriter.write("WHO;\n"); } else if
                         * (fo.getClassAssociation() == 3) {
                         * fileWriter.write("WHAT;\n"); } else if
                         * (fo.getClassAssociation() == 4) {
                         * fileWriter.write("WHEN;\n"); }
                         */
                        else {
                            fileWriter.write("0.0");
                        }
                        fileWriter.write("\n");

                        fileWriter.flush();

                    }

                }

            }

            // fileWriter.write("\n");
            fileWriter.flush();
            fileWriter.close();

        } catch (IOException ex) {
            LOGGER.error(ex);
        }
    }

    /**
     * featureObject 2 HashMap function
     * 
     * @param fo
     *            - the featureObject
     * @return hashMap
     */
    @SuppressWarnings("unused")
    private static Map<String, Double> fo2map(FeatureObject fo) {

        List<String> fn = Arrays.asList(fo.getFeatureNames());
        List<Double> fv = Arrays.asList(fo.getFeatures());

        HashMap<String, Double> hm = new HashMap<String, Double>();

        for (int i = 0; i < fn.size(); i++) {
            hm.put(fn.get(i), fv.get(i));
        }
        return hm;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {

        // String sentence = "NBC Universal drops ad partnership with Google";

        Event event1 = EventExtractor
                .extractEventFromURL("http://articles.latimes.com/2010/oct/14/business/la-fi-ct-nbcgoogle-20101014");
        //
        // Event event = EventExtractor
        // .extractEventFromURL("http://www.bbc.co.uk/news/world-europe-11563423");

        StopWatch sw = new StopWatch();
        sw.start();

        /*
         * POSTagger pt = new POSTagger(POSTagger.POS_TAGGER_OPENNLP);
         * 
         * for (Chunk chunk : getSentenceChunks(event.getText())) { String stc =
         * event.getText() .subSequence(chunk.start(), chunk.end()).toString();
         * pt.tag(stc); List<String> tags = pt.getTags(); List<String> tokens =
         * pt.getTokens(); String tagged = ""; for (int i = 0; i <
         * tokens.size(); i++) { tagged += tokens.get(i) + "/" + tags.get(i) +
         * " "; }
         * 
         * LOGGER.info(tagged);
         * 
         * }
         */

        // CollectionHelper.print(getNounAnnotations("this is my sentence"));

        posTagger.loadModel();
        posTagger.tag(event1.getTitle());
        String s = posTagger.getTaggedString();
        LOGGER.info(s);
        Pattern p;
        try {
            p = Pattern.compile("(.*)/N(.*)/V(.*?)/N(.*)");
        } catch (PatternSyntaxException e) {
            LOGGER.error("Regex syntax error: " + e.getMessage());
            LOGGER.error("Error description: " + e.getDescription());
            LOGGER.error("Error index: " + e.getIndex());
            LOGGER.error("Erroneous pattern: " + e.getPattern());
            return;
        }
        Matcher m = p.matcher(s);

        while (m.find()) {
            for (int i = 1; i <= m.groupCount(); i++) {
                LOGGER.info("" + m.group(i) + " (" + m.start(i) + ","
                        + m.end(i) + ")");

            }
        }

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
