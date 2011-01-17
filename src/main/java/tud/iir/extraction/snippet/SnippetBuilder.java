package tud.iir.extraction.snippet;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.control.Controller;
import tud.iir.extraction.content.PageContentExtractor;
import tud.iir.extraction.content.PageContentExtractorException;
import tud.iir.helper.DataHolder;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Snippet;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;
import tud.iir.web.WebResult;

import com.aliasi.hmm.HiddenMarkovModel;
import com.aliasi.hmm.HmmDecoder;
import com.aliasi.tokenizer.IndoEuropeanTokenizerFactory;
import com.aliasi.tokenizer.Tokenizer;
import com.aliasi.tokenizer.TokenizerFactory;

/**
 * <p>
 * The SnippetBuilder class provides different snippet extraction techniques through a homogeneous extraction function
 * extractSnippets().
 * </p>
 * 
 * <p>
 * Currently implemented are WEBRESULT_SUMMARY, DOCUMENT_SENTENCES and DOCUMENT_SNIPPETS. All these techniques have in
 * common that they receive the Entity and an AggregatedResult as input and return a set of Snippets.
 * </p>
 * 
 * @author Christopher Friedrich
 * @author David Urbansky
 */
public class SnippetBuilder {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(SnippetBuilder.class);

    /** Use the PageContentExtractor to get the main content sections of the page. */
    private PageContentExtractor pageContentExtractor = new PageContentExtractor();

    /**
     * Return a list of sentences of the main content block of the AggregatedResult as snippets extracted, where each
     * sentence must at least consist of a verb
     * and two nouns and must start with the entity in question and and uppercased letter.
     * 
     * @param entity The entity for which to extract snippets.
     * @param webresult The webresult to extract snippets from.
     * @return List of snippets.
     */
    public List<Snippet> extractSnippets(Entity entity, WebResult webresult) {

        ArrayList<Snippet> results = new ArrayList<Snippet>();

        // get sentences with entity
        String text = "";
        String url = webresult.getSource().getUrl();
        try {
            text = pageContentExtractor.setDocument(url).getResultText();
        } catch (PageContentExtractorException e) {
            LOGGER.error("could not get result text for url: " + url + ", " + e.getMessage());
        }
        if (text == null) {
            return null;
        }

        text = removeBIAS(text);

        List<String> sentences = tud.iir.helper.Tokenizer.getSentences(text);

        for (String sentence : sentences) {

            if (sentence == null || sentence.length() == 0) {
                continue;
            }

            if (containsEntity(sentence, entity)) {

                boolean startsWithUpperCase = sentence.substring(0, 1).matches("[A-Z]");
                int verbCount = 0;
                int nounCount = 0;

                List<String> posTags = extractPOSFromSentence(sentence);

                for (String x : posTags) {
                    if (x.startsWith("n")) {
                        nounCount++;
                    } else if (x.startsWith("v") || x.startsWith("b") || x.startsWith("h") || x.startsWith("do")) {
                        verbCount++;
                    }
                }

                if (startsWithUpperCase && verbCount > 0 && nounCount > 1) {

                    Snippet snippet = new Snippet(entity, webresult, sentence);
                    if (snippet.startsWithEntity()) {
                        results.add(snippet);
                    }
                }
            }

        }

        return results;
    }

    /**
     * Extract a list of part-of-speech tags from a sentence.
     * 
     * @param sentence - The sentence
     * @return The part of speach tags.
     */
    @SuppressWarnings("deprecation")
    private List<String> extractPOSFromSentence(String sentence) {

        List<String> tags = null;
        ObjectInputStream oi = null;

        try {

            HiddenMarkovModel hmm = null;

            if (DataHolder.getInstance().containsDataObject("pos-en-general-brown.HiddenMarkovModel")) {
                hmm = (HiddenMarkovModel) DataHolder.getInstance().getDataObject(
                        "pos-en-general-brown.HiddenMarkovModel");
            } else {
                oi = new ObjectInputStream(new FileInputStream(SnippetExtractor.POS_MODEL_PATH));
                hmm = (HiddenMarkovModel) oi.readObject();
                DataHolder.getInstance().putDataObject("pos-en-general-brown.HiddenMarkovModel", hmm);
            }

            HmmDecoder decoder = new HmmDecoder(hmm);

            TokenizerFactory tokenizer_Factory = IndoEuropeanTokenizerFactory.INSTANCE;

            // first get the tokens
            char[] cs = sentence.toCharArray();
            Tokenizer tokenizer = tokenizer_Factory.tokenizer(cs, 0, cs.length);
            String[] tokens = tokenizer.tokenize();

            // then get the tags
            tags = Arrays.asList(decoder.firstBest(tokens));
        } catch (IOException ie) {
            Logger.getRootLogger().error("IO Error: " + ie.getMessage());
        } catch (ClassNotFoundException ce) {
            Logger.getRootLogger().error("Class error: " + ce.getMessage());
        } finally {
            if (oi != null) {
                try {
                    oi.close();
                } catch (IOException ie) {
                    Logger.getRootLogger().error(ie.getMessage());
                }
            }
        }

        return tags;
    }

    /**
     * Count the occurrences of a certain entity in a provided string.
     */
    public int countEntityOccurrences(Entity entity, String text) {
        return getEntityOccurrences(entity, text).size();
    }

    /**
     * Return the set of occurrences of a certain entity in a provided string, including different spellings of the entity.
     * 
     */
    public List<String> getEntityOccurrences(Entity entity, String text) {

        String entityName = entity.getName().toLowerCase();
        text = text.toLowerCase();

        return StringHelper.getRegexpMatches(entityName, text);
    }


    /**
     * Check for occurrences of a certain entity in a provided string.
     */
    private boolean containsEntity(String sentence, Entity entity) {

        if (countEntityOccurrences(entity, sentence) > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Remove certain BIAS from a provided string, such as XML-tags, XML-entities, retweets and twitter tags, URLs and multiple whitespaces.
     */
    private static String removeBIAS(String content) {
        content = StringHelper.makeContinuousText(content);
        return content // .toLowerCase()
                .replaceAll("\\<.*?\\>", "") // any tags, <tag>
                .replaceAll("&(\\w)+;", "") // any xml/html entities, &entity;
                .replaceAll("(RT )?\\@(\\w)+(:)?\\s", "") // any retweets, RT @user:
                .replaceAll("#(\\w)+", "") // any twitter tags, #tag
                .replaceAll("(http\\:)(\\S)+(\\s|$)", "")
                // .replaceAll("(\\s)+", " ") // any multiple whitespaces
                .replaceAll("\\s{2,}", " "); // any multiple whitespaces
    }

    public static void main(String[] abc) {
        Controller.getInstance();

        Entity entity;
        entity = new Entity("iPhone 3GS", new Concept("Mobile Phone"));
        // entity = new Entity("Palm Pre", new Concept("Mobile Phone"));
        // entity = new Entity("Gilroy", new Concept("City"));
        // entity = new Entity("iPhone 3GS", new Concept("Mobile Phone"));
        // entity = new Entity("Reiner Kraft", new Concept("Person"));
        // entity = new Entity("Riesa", new Concept("City"));

        SourceRetriever sr = new SourceRetriever();
        sr.setResultCount(100);
        List<WebResult> wrs = sr.getWebResults(entity.getName(), SourceRetrieverManager.GOOGLE, true);
        // ArrayList<WebResult> wrs = new ArrayList<WebResult>();
        // wrs.add(new WebResult(1, 1, new Source("http://www.google.com"), "title", "summary"));

        for (WebResult wr : wrs) {

            SnippetBuilder sb = new SnippetBuilder();
            List<Snippet> snippets = sb.extractSnippets(entity, wr);
            for (Snippet snippet : snippets) {
                try {
                    System.out.println(snippet.getText() + "\n");
                } catch (Exception e) {
                    System.out.println("OOPS!\n");
                }
            }
        }

        // text startsWithEntity ?

        // SnippetBuilder sb = new SnippetBuilder();
        // Set<Chunk> chunks = sb.getEntityChunks(entity, "The iphone 3gs is good.", true);
        // System.out.println(chunks.toString());
        //		
        // Snippet snippet = new Snippet(entity, ar, "The iPhone 3GS is good.");
        // if (snippet.startsWithEntity()) {
        // System.out.println("yes.");
        // } else {
        // System.out.println("no.");
        // }
    }
}
