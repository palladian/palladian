package tud.iir.classification.page;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.w3c.dom.Document;

import tud.iir.classification.Term;
import tud.iir.helper.StringHelper;
import tud.iir.web.Crawler;

//import edu.unc.ils.PorterStemmer;

/**
 * The preprocessor reads the terms for a given resource and weights them according to their relevance.
 * 
 * 2010-06-09, Philipp, added {@link #preProcessText(String)} and {@link #preProcessText(String, ClassificationDocument)}
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class Preprocessor {

    /** list of words that are not considered in further calculation (English) */
    private String[] stopWords = { "I", "a", "about", "an", "and", "are", "as", "at", "be", "by", "com", "de", "en", "for", "from", "how", "in", "is", "he",
            "she", "it", "la", "of", "on", "or", "that", "the", "this", "to", "was", "what", "when", "where", "who", "will", "with", "und", "the", "www" };

    // characters that will be eliminated
    // private String[] illegalTermCharacters = { "\t", " ", ",", "\"", "'", ":", ";", "\\(", "\\)", "\\.", "\\!", "\\?", "\\{", "\\}" };

    // the weights for the terms that appear in different areas of the resource
    public static final double WEIGHT_DOMAIN_TERM = 8.0;
    public static final double WEIGHT_TITLE_TERM = 7.0;
    public static final double WEIGHT_KEYWORD_TERM = 6.0;
    public static final double WEIGHT_META_TERM = 4.0;
    public static final double WEIGHT_BODY_TERM = 1.0;

    /** maximum number of terms extracted from the resource (for full page preprocessing only) */
    public static final int MAXIMUM_TERMS = 800;

    /** minimum n-gram length (URL preprocessing only) */
    public static final int MIN_NGRAM_SIZE = 4;

    /** maximum n-gram length (URL preprocessing only) */
    public static final int MAX_NGRAM_SIZE = 7;

    // private PorterStemmer porterStemmer = null;

    private Crawler crawler = null;

    /**
     * global map of terms, all documents that are processed by this preprocessor share this term map, this will save memory since strings do not have to be
     * copied but references to the terms will be kept
     */
    private Map<String, Term> termMap = new HashMap<String, Term>();

    /** the term x weight map */
    private Map<Term, Double> map;

    public Preprocessor() {
        // porterStemmer = new PorterStemmer();
        crawler = new Crawler();
    }

    /**
     * Extract terms from keywords of a web page, given in the meta tag "keywords".
     * 
     * @param pageString The website contents.
     */
    private void extractKeywords(org.w3c.dom.Document webPage) {
        ArrayList<String> keywords = Crawler.extractKeywords(webPage);
        for (String term : keywords) {
            String[] keywordTerms = term.split("\\s");
            for (String keywordTerm : keywordTerms) {
                addToTermMap(keywordTerm, WEIGHT_KEYWORD_TERM);
            }
        }
    }

    /**
     * extract terms from the meta description of a web page, given in the meta tag "description"
     * 
     * @param pageString The website contents.
     */
    private void extractMetaDescription(org.w3c.dom.Document webPage) {
        ArrayList<String> keywords = Crawler.extractDescription(webPage);
        for (String term : keywords) {
            addToTermMap(term, WEIGHT_META_TERM);
        }
    }

    /**
     * extract terms from the title of a web page, given in the title tag
     * 
     * @param pageString The website contents.
     */
    private void extractTitle(org.w3c.dom.Document webPage) {
        String title = Crawler.extractTitle(webPage);
        String[] titleWords = title.split("\\s");
        for (String term : titleWords) {
            addToTermMap(term, WEIGHT_TITLE_TERM);
        }
    }

    /**
     * Add a term to the term x weight map. Terms will all be made lowercase.
     * 
     * @param termString The sequence of chars for the term.
     * @param weight The weight of the term.
     */
    private void addToTermMap(String termString, double weight) {

        if (termString.length() < 3 || termString.length() > 18 || map.size() >= MAXIMUM_TERMS || isStopWord(termString)) {
            return;
        }

        termString = StringHelper.trim(termString);

        /*
         * try { term = porterStemmer.stem(term); } catch (StringIndexOutOfBoundsException e) { e.printStackTrace(); }
         */

        if (termString.length() < 3 || termString.length() > 18 || map.size() >= MAXIMUM_TERMS) {
            return;
        }

        termString = termString.toLowerCase();

        Term term = termMap.get(termString);
        if (term == null) {
            term = new Term(termString);
            termMap.put(termString, term);
        }

        if (map.containsKey(term)) {
            double currentWeight = map.get(term);
            map.put(term, currentWeight + weight);
        } else {
            map.put(term, weight);
        }
    }

    private void addToNGramIndex(String ngram, double weight) {

        ngram = ngram.toLowerCase();

        Term term = termMap.get(ngram);
        if (term == null) {
            term = new Term(ngram);
            termMap.put(ngram, term);
        }

        if (map.containsKey(term)) {
            double currentWeight = map.get(term);
            map.put(term, currentWeight + weight);
        } else {
            map.put(term, weight);
        }

    }

    /**
     * Preprocess a string (such as a URL) and create a classification document. A map of n-grams is created for the document and added to it. If a n-gram term
     * exists, it will be taken from the n-gram index.
     * 
     * @param inputString The input string.
     * @param classificationDocument The classification document.
     * @return The classification document with the n-gram map.
     */
    public ClassificationDocument preProcessString(String inputString, ClassificationDocument classificationDocument) {

        // create a new term map for the classification document
        map = new HashMap<Term, Double>();

        // remove http(s): and www from URL
        inputString = Crawler.getCleanURL(inputString);

        HashSet<String> ngrams = StringHelper.calculateAllNGrams(inputString, MIN_NGRAM_SIZE, MAX_NGRAM_SIZE);

        // build the map
        for (String ngram : ngrams) {

            // do not add ngrams with some special chars or if it is only numbers
            if (ngram.indexOf("&") > -1 || ngram.indexOf("/") > -1 || ngram.indexOf("=") > -1 || StringHelper.isNumber(ngram)) {
                continue;
            }

            addToNGramIndex(ngram, 1.0);
        }

        classificationDocument.getWeightedTerms().putAll(map);

        return classificationDocument;
    }

    public ClassificationDocument preProcessString(String url) {
        return preProcessString(url, new ClassificationDocument());
    }

    /**
     * Preprocess a web page and create a classification document. A map of terms is created for the document and added to it. If a term exists, it will be
     * taken from the term index.
     * 
     * @param url The URL of the web page.
     * @param classificationDocument The classification document.
     * @return The classification document with the n-gram map.
     */
    public ClassificationDocument preProcessPage(String url, ClassificationDocument classificationDocument) {

        Document webPage = crawler.getWebDocument(url);

        map = new HashMap<Term, Double>();

        // fill weighted term map with keywords first
        extractKeywords(webPage);

        // get title
        extractTitle(webPage);

        // add meta description keywords
        extractMetaDescription(webPage);

        // get body text
        String bodyContent = Crawler.extractBodyContent(webPage).toLowerCase();

        bodyContent = StringHelper.removeHTMLTags(bodyContent, true, true, true, false);

        // remove stop words
        bodyContent = stripStopWords(bodyContent);

        // get an array of terms
        String[] termArray = bodyContent.split("\\s");

        // build the map, weight 1 for all for now
        for (String term : termArray) {
            addToTermMap(term, WEIGHT_BODY_TERM);
        }

        classificationDocument.getWeightedTerms().putAll(map);

        return classificationDocument;
    }

    public ClassificationDocument preProcessPage(String url) {
        return preProcessPage(url, new ClassificationDocument());
    }

    /**
     * Preprocesses a long string of text similar to {@link #preProcessPage(String, ClassificationDocument)}, but the text content is not downloaded from the
     * web but passed via the url parameter. XXX This is a quick and dirty hack to allow classification of text content and should be refactored somehow in the
     * future.
     * 
     * @author Philipp Katz
     * 
     * @param text the text to be preProcessed
     * @param classificationDocument
     * @return
     */
    public ClassificationDocument preProcessText(String text, ClassificationDocument classificationDocument) {

        map = new HashMap<Term, Double>();

        // remove stop words
        text = stripStopWords(text);

        // get an array of terms
        String[] termArray = text.split("\\s");

        // build the map, weight 1 for all for now
        for (String term : termArray) {
            addToTermMap(term, 1.0);
        }

        classificationDocument.getWeightedTerms().putAll(map);

        return classificationDocument;
    }

    public ClassificationDocument preProcessText(String text) {
        return preProcessText(text, new ClassificationDocument());
    }

    /**
     * Get rid of characters that are not useful for classification purposes.
     * 
     * @param term
     * @return a clean term string without illegal characters
     */
    /*
     * private String removeIllegalCharacters(String term) { // remove all terms that don't have normal characters term = term.replaceAll("^[^a-zA-Z]*$", "");
     * for (int j = 0; j < illegalTermCharacters.length; ++j) { term = term.replaceAll(illegalTermCharacters[j], ""); } return term; }
     */

    /**
     * Strip stop words.
     * 
     * @param words
     * @return a string without words from the stop word list
     */
    private String stripStopWords(String words) {
        for (int i = 0; i < stopWords.length; ++i) {
            words = words.replaceAll("\\s" + stopWords[i] + "\\s", " ");
        }

        return words;
    }

    private boolean isStopWord(String word) {
        word = word.toLowerCase().trim();

        for (String stopWord : stopWords) {
            if (stopWord.equals(word))
                return true;
        }
        return false;
    }

}