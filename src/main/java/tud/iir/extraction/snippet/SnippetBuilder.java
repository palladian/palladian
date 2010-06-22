package tud.iir.extraction.snippet;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import tud.iir.control.Controller;
import tud.iir.helper.StringHelper;
import tud.iir.knowledge.Concept;
import tud.iir.knowledge.Entity;
import tud.iir.knowledge.Snippet;
import tud.iir.web.AggregatedResult;
import tud.iir.web.SourceRetriever;
import tud.iir.web.SourceRetrieverManager;
import tud.iir.web.WebResult;

import com.aliasi.chunk.Chunk;
import com.aliasi.chunk.Chunker;
import com.aliasi.chunk.Chunking;
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
 * The SnippetBuilder class provides different snippet extraction techniques through a homogeneous extraction function extractSnippets().
 * 
 * Currently implemented are WEBRESULT_SUMMARY, DOCUMENT_SENTENCES and DOCUMENT_SNIPPETS. All these techniques have in common that they receive the Entity and
 * an AggregatedResult as input and return a set of Snippets.
 * 
 * This class is described in detail in "Friedrich, Christopher. WebSnippets - Extracting and Ranking of entity-centric knowledge from the Web. Diploma thesis,
 * Technische Universität Dresden, April 2010".
 * 
 * @author Christopher Friedrich
 */
public class SnippetBuilder {

    public static final int WEBRESULT_SUMMARY = 0;
    public static final int DOCUMENT_SENTENCES = 1;
    public static final int DOCUMENT_SNIPPETS = 2;

    /**
     * Extract a list of snippets for the provided Entity from the provided AggregatedResult. This function acts as interface to several extraction techniques
     * implemented.
     * 
     * @param entity - The entity for which to extract snippets.
     * @param webresult - The webresult to extract snippets from.
     * @param method - The technique used for extraction. Currently implemented are WEBRESULT_SUMMARY, DOCUMENT_SENTENCES and DOCUMENT_SNIPPETS as described in
     *            "Friedrich, Christopher. WebSnippets - Extracting and Ranking of entity-centric knowledge from the Web. Diploma thesis, Technische Universit�t
     *            Dresden, April 2010".
     * @return List of snippets
     */
    public List<Snippet> extractSnippets(Entity entity, AggregatedResult webresult, int method) {

        List<Snippet> results = null;

        switch (method) {
            case WEBRESULT_SUMMARY:
                results = extractSnippetsFromSummary(entity, webresult);
                break;
            case DOCUMENT_SENTENCES:
                results = extractSnippetsFromSentence(entity, webresult);
                break;
            case DOCUMENT_SNIPPETS:
                results = extractSnippetsFromDocument(entity, webresult);
                break;
        }

        if (results == null)
            return new ArrayList<Snippet>();

        return results;
    }

    /**
     * Return the summary of the AggregatedResult as only snippet extracted.
     */
    private List<Snippet> extractSnippetsFromSummary(Entity entity, AggregatedResult webresult) {

        ArrayList<Snippet> results = new ArrayList<Snippet>();

        // new Snippet
        Snippet snippet = new Snippet(entity, webresult, removeBIAS(webresult.getWebresults().get(0).getSummary()));
        results.add(snippet);

        return results;
    }

    /**
     * Return the list of sentences of the main content block of the AggregatedResult as snippets extracted.
     */
    private List<Snippet> extractSnippetsFromSentence(Entity entity, AggregatedResult webresult) {

        ArrayList<Snippet> results = new ArrayList<Snippet>();

        // get sentences with entity
        String text = webresult.getSource().getMainContent();
        for (Chunk chunk : getSentenceChunks(webresult.getSource().getMainContent())) {
            String sentence = text.substring(chunk.start(), chunk.end()).trim();
            if (containsEntity(sentence, entity)) {

                // new Snippet
                Snippet snippet = new Snippet(entity, webresult, sentence);

                results.add(snippet);
            }
        }

        return results;
    }

    /**
     * Return a list of sentences of the main content block of the AggregatedResult as snippets extracted, where each sentence must at least consist of a verb
     * and two nouns and must start with the entity in question and and uppercased letter.
     */
    private List<Snippet> extractSnippetsFromDocument(Entity entity, AggregatedResult webresult) {

        // int minSentenceLength = 100;

        ArrayList<Snippet> results = new ArrayList<Snippet>();

        // get sentences with entity
        String text = webresult.getSource().getMainContent();
        if (text == null)
            return null;

        // String prevSentence = null;

        text = removeBIAS(text);

        for (Chunk chunk : getSentenceChunks(text)) {
            String sentence = text.substring(chunk.start(), chunk.end()).trim();

            if (sentence == null || sentence.length() == 0)
                continue;

            if (containsEntity(sentence, entity)) {

                boolean startsWithUpperCase = sentence.substring(0, 1).matches("[A-Z]");
                int verbCount = 0;
                int nounCount = 0;

                List<String> posTags = SnippetFeatureExtractor.extractPOSFromSentence(sentence);

                for (String x : posTags) {
                    if (x.startsWith("n")) {
                        nounCount++;
                    } else if (x.startsWith("v") || x.startsWith("b") || x.startsWith("h") || x.startsWith("do")) {
                        verbCount++;
                    }
                }

                if (startsWithUpperCase && verbCount > 0 && nounCount > 1) {
                    // new Snippet
                    Snippet snippet = new Snippet(entity, webresult, sentence);
                    // if (prevSentence != null && sentence.length() < minSentenceLength) {
                    // snippet.setText(prevSentence + " " + sentence);
                    // } else {
                    // snippet.setText(sentence);
                    // }

                    if (snippet.startsWithEntity()) {
                        results.add(snippet);
                    }
                }
            }
            // prevSentence = sentence;
        }

        return results;
    }

    /**
     * Count the occurrences of a certain entity in a provided string.
     */
    public int countEntityOccurrences(Entity entity, String text) {
        return getEntityChunks(entity, text, false).size();
    }

    /**
     * Return the set of occurrences of a certain entity in a provided string, including different spellings of the entity.
     * 
     * An optional parameter allows to specify whether the entity might be prefixed by "the", "an" or "a".
     */
    public Set<Chunk> getEntityChunks(Entity entity, String text, boolean includePrefixes) {

        // lowercase everything

        String entityName = entity.getName().toLowerCase();
        text = text.toLowerCase();

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
        dict.addEntry(new DictionaryEntry<String>(entityName, entity.getName()));
        if (includePrefixes) {
            for (String prefix : prefixes) {
                dict.addEntry(new DictionaryEntry<String>(prefix + " " + entityName, entity.getName()));
            }
        }

        // synonyms
        for (String synonym : synonyms) {
            dict.addEntry(new DictionaryEntry<String>(synonym, entity.getName()));
            if (includePrefixes) {
                for (String prefix : prefixes) {
                    dict.addEntry(new DictionaryEntry<String>(prefix + " " + synonym, entity.getName()));
                }
            }
        }

        WeightedEditDistance editDistance = new FixedWeightEditDistance(0, -1, -1, -1, Double.NaN);

        Chunker chunker = new ApproxDictionaryChunker(dict, IndoEuropeanTokenizerFactory.INSTANCE, editDistance, maxDistance);

        return chunker.chunk(text).chunkSet();
    }

    /**
     * Split a provided string into sentences and return a set of sentence chunks.
     */
    private Set<Chunk> getSentenceChunks(String text) {

        if (text == null)
            return null;

        TokenizerFactory tokenizerFactory = IndoEuropeanTokenizerFactory.INSTANCE;
        SentenceModel sentenceModel = new IndoEuropeanSentenceModel();

        SentenceChunker sentenceChunker = new SentenceChunker(tokenizerFactory, sentenceModel);
        Chunking chunking = sentenceChunker.chunk(text.toCharArray(), 0, text.length());

        return chunking.chunkSet();
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
        ArrayList<WebResult> wrs = sr.getWebResults(entity.getName(), SourceRetrieverManager.GOOGLE, true);
        // ArrayList<WebResult> wrs = new ArrayList<WebResult>();
        // wrs.add(new WebResult(1, 1, new Source("http://www.google.com"), "title", "summary"));

        for (WebResult wr : wrs) {

            ArrayList<WebResult> webresults = new ArrayList<WebResult>();
            webresults.add(wr);

            AggregatedResult ar = new AggregatedResult(webresults, 1);
            System.out.println("\n\tURL: " + ar.getSource().getUrl() + "\n");

            SnippetBuilder sb = new SnippetBuilder();
            List<Snippet> snippets = sb.extractSnippets(entity, ar, DOCUMENT_SNIPPETS);
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
