package tud.iir.extraction.event;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import tud.iir.daterecognition.DateEvaluator;
import tud.iir.daterecognition.DateGetter;
import tud.iir.daterecognition.dates.ExtractedDate;
import tud.iir.extraction.entity.ner.Annotation;
import tud.iir.extraction.entity.ner.Annotations;
import tud.iir.extraction.entity.ner.tagger.LingPipeNER;
import tud.iir.extraction.entity.ner.tagger.OpenNLPNER;
import tud.iir.helper.ConfigHolder;
import tud.iir.helper.StringHelper;
import tud.iir.preprocessing.nlp.NaturalLanguageProcessor;
import tud.iir.preprocessing.nlp.OpenNLPPOSTagger;
import tud.iir.preprocessing.nlp.OpenNLPParser;
import tud.iir.preprocessing.nlp.OpenNLPPhraseChunker;
import tud.iir.preprocessing.nlp.OpenNLPSentenceDetector;
import tud.iir.preprocessing.nlp.TagAnnotation;
import tud.iir.preprocessing.nlp.TagAnnotations;
import weka.core.stemmers.SnowballStemmer;

import com.aliasi.coref.EnglishMentionFactory;
import com.aliasi.coref.Mention;
import com.aliasi.coref.MentionFactory;
import com.aliasi.coref.WithinDocCoref;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

public class EventProcessor extends NaturalLanguageProcessor {

    /** the logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EventProcessor.class);

    /**
     * @param args
     */
    public static void main(String[] args) {
        EventProcessor eventProcessor = new EventProcessor();
        eventProcessor.init();

        eventProcessor.processEvent(new Event(
                "http://www.bbc.co.uk/news/world-africa-12206377"));

    }

    /**
     * prepares the processor by loading its parts.
     */
    protected void init() {

        this.setNer(new LingPipeNER());
        this.setPosTagger(new OpenNLPPOSTagger());
        this.setPhraseChunker(new OpenNLPPhraseChunker());
        this.setSentenceDetector(new OpenNLPSentenceDetector());
        this.setParser(new OpenNLPParser());

        this.loadDefaultModels();

    }

    private void loadDefaultModels() {
        PropertiesConfiguration config = null;

        config = ConfigHolder.getInstance().getConfig();

        if (config != null) {

            ner.loadModel(config.getString("models.lingpipe.en.ner"));
            posTagger.loadDefaultModel();
            phraseChunker.loadDefaultModel();
            sentenceDetector.loadDefaultModel();
            parser.loadDefaultModel();

        } else {
            LOGGER.error("Error while loading palladian config file.");
        }

    }

    public void processEvent(Event event) {

        if (event.getText().length() > 0) {
            event.setSentences(getSentences(event.getText()));
            annotateEvent(event);
        } else {
            LOGGER
                    .info("Error while proccessing event, is the event already created from url?");
        }

    }

    /**
     * Performing co-reference resolution.
     * 
     * @param event
     * @return
     */
    public Map<Integer, Annotations> getCoreferenceAnnotations(
            Annotations annotations) {

        /*
         * final StopWatch stopWatch = new StopWatch(); stopWatch.start();
         */
        final MentionFactory mfactory = new EnglishMentionFactory();
        final WithinDocCoref coref = new WithinDocCoref(mfactory);

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

        return corefAnnotationMap;

    }

    /**
     * performs Namend Entity Recognition on the given event and annotates nouns
     * in headline.
     * 
     * @param event
     */
    private void annotateEvent(Event event) {

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

        // now performing coreference annotation

        event
                .setCorefAnnotations((HashMap<Integer, Annotations>) getCoreferenceAnnotations(event
                        .getAnnotations()));

    }

    /**
     * Annotates NounPhrases in Title and first Sentence.
     * 
     * @param event
     * @return the annotations
     */
    public Annotations getNounAnnotations(String text) {

        phraseChunker.loadDefaultModel();
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

    /**
     * annotates Dates by the OpenNLP NER.
     * 
     * @param text
     * @return annotations
     */
    public Annotations getDateAnnotations(String text) {
        final OpenNLPNER timeNer = new OpenNLPNER();

        PropertiesConfiguration config = null;
        config = ConfigHolder.getInstance().getConfig();

        if (config != null) {
            timeNer.loadModel(config.getString("models.opennlp.en.ner.time")
                    + "," + config.getString("models.opennlp.en.ner.date"));
        } else {
            LOGGER.error("Error while reading models.");
        }
        return timeNer.getAnnotations(text);

    }

    /**
     * @param event
     * @return
     */
    public HashMap<ExtractedDate, Double> getRatedDates(Event event) {
        final DateEvaluator dr = new DateEvaluator();
        HashMap<ExtractedDate, Double> ratedDates = new HashMap<ExtractedDate, Double>();

        if (event.getUrl() != null && event.getDocument() != null) {
            ArrayList<ExtractedDate> dates = new ArrayList<ExtractedDate>();
            final DateGetter dateGetter = new DateGetter(event.getUrl(), event
                    .getDocument());
            dateGetter.setAllFalse();
            dateGetter.setTechHTMLContent(true);
            dateGetter.setTechHTMLHead(true);
            dateGetter.setTechHTMLStruct(true);
            // dateGetter.setTechReference(true);

            dates = dateGetter.getDate();

            ratedDates = dr.rate(dates);

        }
        return ratedDates;
    }

    public Map<String, Double> getWhyRegExp(String who, String what) {

        final Map<String, Double> regExpMap = new HashMap<String, Double>();

        regExpMap.put("(" + what + "(.*)to/TO [^ \t](.*)/VB)", 0.5);
        // regExpMap.put("(" + who + "(.*)to/TO [^ \t](.*)/VB)", 0.5);
        regExpMap.put("(" + what + "(.*)will)", 0.5);
        regExpMap.put("(since)", 0.2);
        regExpMap.put("(cause)", 0.3);
        regExpMap.put("(because)", 0.3);
        regExpMap.put("(hence)", 0.2);
        regExpMap.put("(therefore)", 0.3);
        regExpMap.put("(why)", 0.2);
        regExpMap.put("(result)", 0.4);
        regExpMap.put("(reason)", 0.3);
        regExpMap.put("(provide)", 0.1);
        regExpMap.put("('s behind)", 0.2);
        regExpMap.put("(Due to)", 0.2);

        return regExpMap;
    }

    /**
     * @param sentence
     * @param word
     * @return
     */
    public String getSubsequentVerbPhrase(String sentence, String word) {

        final ArrayList<String> verbPhrases = new ArrayList<String>();
        boolean foundNoun = false;
        // boolean foundVerb = false;
        for (final TagAnnotation tagAnnotation : getParse(sentence)) {

            if (foundNoun && tagAnnotation.getTag().contains("V")
                    && tagAnnotation.getChunk().length() < 150) {
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
        return verbPhrases.size() > 0 ? verbPhrases.get(0) : null;
    }

    /**
     * @param str1
     * @param str2
     * @return
     */
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
     * @param annotations
     * @return
     */
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
     * returns the shortest term from a list of annotations.
     * 
     * @param annotations
     * @return shortest term
     */
    public String shortestTerm(Annotations annotations) {

        String term = "longlonglonglonglonglong";

        for (final Annotation annotation : annotations) {
            if (term.length() > annotation.getLength()) {
                term = annotation.getEntity().getName();
            }

        }
        return term;

    }

}
