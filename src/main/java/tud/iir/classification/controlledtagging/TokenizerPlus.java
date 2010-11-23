package tud.iir.classification.controlledtagging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.Factory;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.collections15.map.LazyMap;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.w3c.dom.Document;

import tud.iir.classification.Stopwords;
import tud.iir.extraction.event.AbstractPOSTagger;
import tud.iir.extraction.event.LingPipePOSTagger;
import tud.iir.extraction.event.TagAnnotation;
import tud.iir.extraction.event.TagAnnotations;
import tud.iir.helper.CollectionHelper;
import tud.iir.helper.CountMap;
import tud.iir.helper.HTMLHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.Tokenizer;
import tud.iir.web.Crawler;

/**
 * Special tokenizer implementation which keeps positional and POS features of extracted Tokens. Also supports
 * extraction of collocated terms, like "San Francisco" or "domestic cat". Experimental.
 * 
 * @author Philipp Katz
 * 
 */
public class TokenizerPlus {

    // TODO only load on demand.
    private AbstractPOSTagger posTagger = new LingPipePOSTagger();
    private SnowballStemmer stemmer = new englishStemmer();
    private boolean usePosTagging = true;
    private Set<String> stopwords = new Stopwords(Stopwords.Predefined.EN);

    public TokenizerPlus() {
        posTagger.loadModel();
    }

    public List<Token> tokenize(String text) {
        List<Token> tokens;
        // first, tokenize into sentences.
        List<String> sentences = Tokenizer.getSentences(text);
        // if we have no sentences, we just take the whole text.
        if (sentences.size() == 0) {
            sentences.add(text);
        }
        // either tokenize with or without POS tagging
        // POS tagging takes time
        if (usePosTagging) {
            tokens = tokenizePos(sentences);
        } else {
            tokens = tokenizePlain(sentences);
        }
        return tokens;
    }

    private List<Token> tokenizePos(List<String> sentences) {
        List<Token> tokens = new ArrayList<Token>();
        int textPosition = 0;
        int sentencePosition = 0;
        int sentenceNumber = 0;
        for (String sentence : sentences) {
            posTagger.tag(sentence);
            TagAnnotations tagAnnotations = posTagger.getTagAnnotations();
            for (TagAnnotation tagAnnotation : tagAnnotations) {
                String string = tagAnnotation.getChunk();
                String tag = tagAnnotation.getTag();
                Token token = new Token();
                token.setUnstemmedValue(string);
                token.setStemmedValue(stem(string));
                token.setTextPosition(textPosition);
                token.setSentencePosition(sentencePosition);
                token.setSentenceNumber(sentenceNumber);
                token.setPosTag(tag);
                tokens.add(token);
                sentencePosition++;
                textPosition++;
            }
            sentencePosition = 0;
            sentenceNumber++;
        }
        return tokens;
    }

    private List<Token> tokenizePlain(List<String> sentences) {
        List<Token> tokens = new ArrayList<Token>();
        int textPosition = 0;
        int sentencePosition = 0;
        int sentenceNumber = 0;
        for (String sentence : sentences) {
            List<String> sentenceTokens = Tokenizer.tokenize(sentence);
            for (String string : sentenceTokens) {
                Token token = new Token();
                token.setUnstemmedValue(string);
                token.setStemmedValue(stem(string));
                token.setTextPosition(textPosition);
                token.setSentencePosition(sentencePosition);
                token.setSentenceNumber(sentenceNumber);
                tokens.add(token);
                sentencePosition++;
                textPosition++;
            }
            sentencePosition = 0;
            sentenceNumber++;
        }
        return tokens;
    }

    private String stem(String unstemmed) {
        stemmer.setCurrent(unstemmed.toLowerCase());
        stemmer.stem();
        return stemmer.getCurrent();
    }

    /**
     * Extract collocations from supplied token list, based on some simple heuristics. In our terms, a collocation is a
     * sequence of terms, appearing frequently throughout the text, like "San Francisco", not beginning or ending with a
     * stopword. This method is somewhat sophisticated, but I have no idea how to simplify it. It contains some magic
     * numbers which proved to work well for longer texts, but which still need further evaluation.
     * 
     * @param tokens
     * @return
     */
    public List<Token> makeCollocations(List<Token> tokens, int maxLength) {

        List<Token> result = new ArrayList<Token>();

        // keep stems + count of all correlations which we extracted
        Bag<String> resultStems = new HashBag<String>();

        // array of tokens for n-gram creation
        Token[] tokenArray = tokens.toArray(new Token[tokens.size()]);

        // keep single stemmed tokens in this bag,
        // we need this to look up their occurrence counts
        Bag<String> stemmedTokenBag = new HashBag<String>();
        for (Token token : tokens) {
            stemmedTokenBag.add(token.getStemmedValue());
        }

        // lazy map which never returns null when get() is invoked;
        // if specified key does not exist, it is created by the factory
        Map<String, List<Token>> collocationMap = LazyMap.decorate(new HashMap<String, List<Token>>(),
                new Factory<List<Token>>() {
                    @Override
                    public List<Token> create() {
                        return new ArrayList<Token>();
                    }
                });

        // gram size. do a reverse iteration,
        // as we prefer longer collocations
        for (int n = maxLength; n >= 2; n--) {

            // build n-grams
            for (int i = 0; i <= tokenArray.length - n; i++) {

                // keep space separated token values
                StringBuilder unstemmedBuilder = new StringBuilder();
                StringBuilder stemmedBuilder = new StringBuilder();

                // keep the sentence position of the last checked token,
                // this way we avoid creating collocations which span sentence
                // boundaries.
                int lastSentencePosition = 0;
                boolean accept = true;

                // don't accept collocations which start or end with a stopword
                accept = accept && !stopwords.contains(tokenArray[i].getUnstemmedValue());
                accept = accept && !stopwords.contains(tokenArray[i + n - 1].getUnstemmedValue());

                if (!accept) {
                    continue;
                }

                for (int j = i; j < i + n; j++) {

                    Token currentToken = tokenArray[j];

                    boolean spansSentence = lastSentencePosition > currentToken.getSentencePosition();
                    boolean matchesPattern = currentToken.getStemmedValue().matches("[A-Za-z0-9]{2,}");

                    if (spansSentence || !matchesPattern) {
                        accept = false;
                        break;
                    }

                    lastSentencePosition = currentToken.getSentencePosition();
                    unstemmedBuilder.append(currentToken.getUnstemmedValue()).append(" ");
                    stemmedBuilder.append(currentToken.getStemmedValue()).append(" ");

                }

                if (accept) {

                    Token collocation = new Token();
                    String stemmedValue = stemmedBuilder.toString().trim();

                    // set values from first token in this collocation
                    collocation.setTextPosition(tokenArray[i].getTextPosition());
                    collocation.setSentenceNumber(tokenArray[i].getSentenceNumber());
                    collocation.setSentencePosition(tokenArray[i].getSentencePosition());
                    collocation.setUnstemmedValue(unstemmedBuilder.toString().trim());
                    collocation.setStemmedValue(stemmedValue);

                    List<Token> existingCollocations = collocationMap.get(stemmedValue);
                    existingCollocations.add(collocation);

                }

            }

            // filter out the collocations, which we really want to keep
            Set<Entry<String, List<Token>>> entries = collocationMap.entrySet();
            for (Entry<String, List<Token>> entry : entries) {

                String stemmedValue = entry.getKey();
                List<Token> entryTokens = entry.getValue();
                int occurences = entryTokens.size();
                String[] singleStems = stemmedValue.split(" ");

                // determine minimum # of occurrences over all stems
                int minCount = Integer.MAX_VALUE;
                for (String stem : singleStems) {
                    minCount = Math.min(minCount, stemmedTokenBag.getCount(stem));
                }

                // check, if we already have it as a "more complete" collocation;
                // for example "New York Times" is more complete than "York Times".
                boolean accept = true;
                for (String existingStem : resultStems.uniqueSet()) {
                    if (existingStem.contains(stemmedValue) && resultStems.getCount(existingStem) >= occurences * 0.55) { // TODO evaluate
                        accept = false;
                        break;
                    }
                }

                // the score determines, how many times a term appears with the specific collocation
                float score = (float) occurences / minCount;
                if (accept && occurences > 2 && score > 0.5) { // TODO evaluate
                    result.addAll(entryTokens);
                    resultStems.add(stemmedValue, occurences);
                }

            }

        }

        // // sort tokens by position
        // Comparator<Token> tokenPositionComparator = new Comparator<Token>() {
        // @Override
        // public int compare(Token t1, Token t2) {
        // return new Integer(t1.getTextPosition()).compareTo(t2.getTextPosition());
        // }
        // };
        // Collections.sort(result, tokenPositionComparator);

        return result;

    }
    
    public boolean isUsePosTagging() {
        return usePosTagging;
    }
    
    public void setUsePosTagging(boolean usePosTagging) {
        this.usePosTagging = usePosTagging;
    }

    public static void main(String[] args) {

        TokenizerPlus tokenizer = new TokenizerPlus();
        tokenizer.usePosTagging = false;

        Crawler crawler = new Crawler();
        // Document doc = crawler.getWebDocument("http://en.wikipedia.org/wiki/Apple_iPhone");
        // Document doc = crawler.getWebDocument("http://en.wikipedia.org/wiki/San_Francisco");
        // Document doc = crawler.getWebDocument("http://en.wikipedia.org/wiki/%22Manos%22_The_Hands_of_Fate");
        // Document doc = crawler.getWebDocument("http://en.wikipedia.org/wiki/Cat");
        // Document doc = crawler.getWebDocument("http://edition.cnn.com/2010/TECH/social.media/11/19/social.media.isolation.project/index.html?hpt=C1");
        // Document doc = crawler.getWebDocument("http://blogs.reuters.com/mediafile/2010/11/18/ft-hearts-tablets-so-much-its-spreading-the-joy-among-staff/");
        Document doc = crawler.getWebDocument("http://en.wikipedia.org/wiki/The_Garden_of_Earthly_Delights");
        String text = HTMLHelper.htmlToString(doc);

        // String text = "the quick brown fox jumps over the lazy dog. brown foxes. brown fox. brown fox. fox";
        StopWatch sw = new StopWatch();
        List<Token> tokens = tokenizer.tokenize(text);
        System.out.println("# of tokens : " + tokens.size());
        List<Token> collocations = tokenizer.makeCollocations(tokens, 5);

        List<Token> allTokens = new ArrayList<Token>();
        // allTokens.addAll(tokens);
        allTokens.addAll(collocations);
        
        CountMap cm = new CountMap();
        for (Token token : allTokens) {
            cm.increment(token.getStemmedValue());
        }
        
        
        
        LinkedHashMap<Object, Integer> sort = CollectionHelper.sortByValue(cm.entrySet(), false);
        for (Entry<Object, Integer> entry : sort.entrySet()) {
            System.out.println(entry.getValue() + " " + entry.getKey());
        }
        System.out.println(sw.getElapsedTimeString());

        // CollectionHelper.print(tokens);

    }

}
