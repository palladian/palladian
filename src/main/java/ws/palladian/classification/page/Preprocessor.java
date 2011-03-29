package ws.palladian.classification.page;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Document;

import ws.palladian.classification.Term;
import ws.palladian.classification.page.evaluation.FeatureSetting;
import ws.palladian.extraction.PageAnalyzer;
import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.HTMLHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.helper.nlp.Tokenizer;
import ws.palladian.retrieval.DocumentRetriever;

/**
 * The preprocessor reads the terms for a given resource and weights them according to their relevance.
 * 
 * 2010-06-09, Philipp, added {@link #preProcessText(String)} and {@link #preProcessText(String, TextInstance)}
 * 
 * @author David Urbansky
 * @author Philipp Katz
 */
public final class Preprocessor implements Serializable {

    /** The serialize version ID. */
    private static final long serialVersionUID = -7623884004056059738L;

    /** The weights for the terms that appear in different areas of the resource. */
    public static final double WEIGHT_DOMAIN_TERM = 8.0;
    public static final double WEIGHT_TITLE_TERM = 7.0;
    public static final double WEIGHT_KEYWORD_TERM = 6.0;
    public static final double WEIGHT_META_TERM = 4.0;
    public static final double WEIGHT_BODY_TERM = 1.0;

    /** The crawler. */
    private transient DocumentRetriever crawler = null;

    /**
     * the classifier that this preprocessor belongs to, the classifier holds the feature settings which are needed here
     */
    private TextClassifier classifier;

    /**
     * Global map of terms, all documents that are processed by this preprocessor share this term map, this will save
     * memory since strings do not have to be
     * copied but references to the terms will be kept.
     */
    private transient Map<String, Term> termMap = new HashMap<String, Term>();

    /** The term x weight map. */
    private transient Map<Term, Double> map;

    public Preprocessor(TextClassifier classifier) {
        this.classifier = classifier;
        crawler = new DocumentRetriever();
    }

    /**
     * Extract terms from keywords of a web page, given in the meta tag "keywords".
     * 
     * @param pageString The website contents.
     */
    private void extractKeywords(org.w3c.dom.Document webPage) {
        List<String> keywords = PageAnalyzer.extractKeywords(webPage);
        for (String term : keywords) {
            String[] keywordTerms = term.split("\\s");
            for (String keywordTerm : keywordTerms) {
                addToTermMap(keywordTerm, WEIGHT_KEYWORD_TERM);
            }
        }
    }

    /**
     * Extract terms from the meta description of a web page, given in the meta tag "description".
     * 
     * @param pageString The website contents.
     */
    private void extractMetaDescription(org.w3c.dom.Document webPage) {
        List<String> keywords = PageAnalyzer.extractDescription(webPage);
        for (String term : keywords) {
            addToTermMap(term, WEIGHT_META_TERM);
        }
    }

    /**
     * Extract terms from the title of a web page, given in the title tag.
     * 
     * @param pageString The website contents.
     */
    private void extractTitle(org.w3c.dom.Document webPage) {
        String title = PageAnalyzer.extractTitle(webPage);
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

        if (getFeatureSetting().getTextFeatureType() == FeatureSetting.WORD_NGRAMS
                && getFeatureSetting().getMaxNGramLength() == 1 && (termString.length() < getFeatureSetting()
                        .getMinimumTermLength() || termString.length() > getFeatureSetting().getMaximumTermLength())
                        || map.size() >= getFeatureSetting().getMaxTerms()
                        || isStopWord(termString)) {
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

    private FeatureSetting getFeatureSetting() {
        return classifier.getFeatureSetting();
    }

    /**
     * Preprocess a string (such as a URL) and create a classification document. A map of n-grams is created for the document and added to it. If a n-gram term
     * exists, it will be taken from the n-gram index.
     * 
     * @param inputString The input string.
     * @param classificationDocument The classification document.
     * @return The classification document with the n-gram map.
     */
    public TextInstance preProcessDocument(String inputString, TextInstance classificationDocument) {

        // the term map is transient and might need to be initialized
        if (termMap == null) {
            termMap = new HashMap<String, Term>();
        }

        // create a new term map for the classification document
        map = new HashMap<Term, Double>();

        // remove http(s): and www from URL XXX
        inputString = UrlHelper.getCleanURL(inputString);

        Set<String> ngrams = null;

        if (getFeatureSetting().getTextFeatureType() == FeatureSetting.CHAR_NGRAMS) {

            ngrams = Tokenizer.calculateAllCharNGrams(inputString, getFeatureSetting().getMinNGramLength(),
                    getFeatureSetting().getMaxNGramLength());

        } else if (getFeatureSetting().getTextFeatureType() == FeatureSetting.WORD_NGRAMS) {

            ngrams = Tokenizer.calculateAllWordNGrams(inputString, getFeatureSetting().getMinNGramLength(),
                    getFeatureSetting().getMaxNGramLength());

        }

        // build the map
        for (String ngram : ngrams) {

            // TODO, change that => do not add ngrams with some special chars or if it is only numbers
            if (ngram.indexOf("&") > -1 || ngram.indexOf("/") > -1 || ngram.indexOf("=") > -1
                    || StringHelper.isNumber(ngram)) {
                continue;
            }

            addToTermMap(ngram, 1.0);
        }

        classificationDocument.getWeightedTerms().putAll(map);

        return classificationDocument;
    }

    public TextInstance preProcessDocument(String url) {
        return preProcessDocument(url, new TextInstance());
    }

    /**
     * Preprocess a string (such as a URL) and create a classification document. A map of n-grams is created for the
     * document and added to it. If a n-gram term
     * exists, it will be taken from the n-gram index.
     * 
     * @deprecated consider using preprocess document
     * 
     * @param inputString The input string.
     * @param classificationDocument The classification document.
     * @return The classification document with the n-gram map.
     */
    @Deprecated
    public TextInstance preProcessString(String inputString, TextInstance classificationDocument) {

        // create a new term map for the classification document
        map = new HashMap<Term, Double>();

        // remove http(s): and www from URL
        inputString = UrlHelper.getCleanURL(inputString);

        Set<String> ngrams = Tokenizer.calculateAllCharNGrams(inputString, getFeatureSetting().getMinNGramLength(),
                getFeatureSetting().getMaxNGramLength());

        // build the map
        for (String ngram : ngrams) {

            // do not add ngrams with some special chars or if it is only numbers
            if (ngram.indexOf("&") > -1 || ngram.indexOf("/") > -1 || ngram.indexOf("=") > -1 || StringHelper.isNumber(ngram)) {
                continue;
            }

            addToTermMap(ngram, 1.0);
        }

        classificationDocument.getWeightedTerms().putAll(map);

        return classificationDocument;
    }

    public TextInstance preProcessString(String url) {
        return preProcessString(url, new TextInstance());
    }

    /**
     * Preprocess a web page and create a classification document. A map of terms is created for the document and added
     * to it. If a term exists, it will be
     * taken from the term index.
     * 
     * @deprecated consider using preprocess document
     * 
     * @param url The URL of the web page.
     * @param classificationDocument The classification document.
     * @return The classification document with the n-gram map.
     */
    @Deprecated
    public TextInstance preProcessPage(String url, TextInstance classificationDocument) {

        Document webPage = crawler.getWebDocument(url);

        map = new HashMap<Term, Double>();

        // fill weighted term map with keywords first
        extractKeywords(webPage);

        // get title
        extractTitle(webPage);

        // add meta description keywords
        extractMetaDescription(webPage);

        // get body text
        String bodyContent = PageAnalyzer.extractBodyContent(webPage).toLowerCase();

        bodyContent = HTMLHelper.stripHTMLTags(bodyContent, true, true, true, false);

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

    /**
     * @deprecated consider using preprocess document
     * @param url
     * @return
     */
    @Deprecated
    public TextInstance preProcessPage(String url) {
        return preProcessPage(url, new TextInstance());
    }

    /**
     * Preprocesses a long string of text similar to {@link #preProcessPage(String, TextInstance)}, but the
     * text content is not downloaded from the
     * web but passed via the url parameter. XXX This is a quick and dirty hack to allow classification of text content
     * and should be refactored somehow in the
     * future.
     * 
     * @deprecated consider using preprocess document
     * 
     * @author Philipp Katz
     * 
     * @param text the text to be preProcessed
     * @param classificationDocument
     * @return
     */
    @Deprecated
    public TextInstance preProcessText(String text, TextInstance classificationDocument) {

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

    /**
     * @deprecated consider using preprocess document
     * @param text
     * @return
     */
    @Deprecated
    public TextInstance preProcessText(String text) {
        return preProcessText(text, new TextInstance());
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
        for (String stopWord : getFeatureSetting().getStopWords()) {
            words = words.replaceAll("\\s" + stopWord + "\\s", " ");
        }

        return words;
    }

    private boolean isStopWord(String word) {
        word = word.toLowerCase().trim();

        for (String stopWord : getFeatureSetting().getStopWords()) {
            if (stopWord.equals(word)) {
                return true;
            }
        }
        return false;
    }

}