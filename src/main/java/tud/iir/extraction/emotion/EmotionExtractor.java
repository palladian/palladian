package tud.iir.extraction.emotion;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.log4j.Logger;

import tud.iir.extraction.Extractor;
import tud.iir.extraction.content.PageSentenceExtractor;
import tud.iir.helper.FileHelper;
import tud.iir.helper.Tokenizer;

public class EmotionExtractor extends Extractor {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(EmotionExtractor.class);

    /** The vocabulary for each emotion: emotion:[word1,...,wordN]. */
    private Map<String, Set<String>> vocabulary;

    private EmotionExtractor() {

        // do not analyze any binary files
        addSuffixesToBlackList(Extractor.URL_BINARY_BLACKLIST);

        loadVocabulary("config/emotionDictionary_german.csv");
    }

    static class SingletonHolder {
        static EmotionExtractor instance = new EmotionExtractor();
    }

    public static EmotionExtractor getInstance() {
        return SingletonHolder.instance;
    }

    public void loadVocabulary(String dictionaryPath) {

        LOGGER.info("loading vocabulary from " + dictionaryPath);

        List<String> fileEntries = FileHelper.readFileToArray(dictionaryPath);
        vocabulary = new HashMap<String, Set<String>>();

        for (String string : fileEntries) {
            String[] parts = string.split(";");

            if (parts.length != 2) {
                continue;
            }

            Set<String> emotionWords = vocabulary.get(parts[1]);
            if (emotionWords == null) {
                emotionWords = new HashSet<String>();
                vocabulary.put(parts[1], emotionWords);
            }
            emotionWords.add(parts[0]);
        }

        LOGGER.info("loaded words for " + vocabulary.size() + " emotions");
    }

    public void startExtraction() {

    }

    /**
     * Extract emotions about a given entity from a given URL.
     * 
     * @param entityName The name of the entity (a product name or company name) for which emotions should be extracted.
     * @param url The URL from which the emotions should be extracted.
     */
    public void extractEmotions(EntityEmotionDistribution eed, String url) {
        PageSentenceExtractor pse = new PageSentenceExtractor();
        pse.setDocument(url);
        String text = pse.getSentencesString();
        extractEmotions(eed, text, url);
    }

    /**
     * Extract emotions about a given entity from a given text.
     * 
     * @param entityName The name of the entity (a product name or company name) for which emotions should be extracted.
     * @param url The URL from which the emotions should be extracted.
     */
    public void extractEmotions(EntityEmotionDistribution eed, String text, String url) {

        List<String> sentences = Tokenizer.getSentences(text);

        // the token before the current one
        String previousToken1 = null;

        // the token two tokens before the current one
        String previousToken2 = null;

        // iterate over all sentences
        for (String sentence : sentences) {

            List<String> tokens = new ArrayList<String>();

            tokens = Tokenizer.tokenize(sentence);

            // iterate over all tokens of the sentence
            for (String token : tokens) {

                // iterate over complete vocabulary
                for (Entry<String, Set<String>> vocabularyEntry : vocabulary.entrySet()) {

                    String emotion = vocabularyEntry.getKey();

                    // compare each word in the vocabulary with the current token
                    for (String emotionWord : vocabularyEntry.getValue()) {

                        emotionWord = emotionWord.toLowerCase();
                        token = token.toLowerCase();

                        int tokenLength = token.length();
                        int emotionWordLength = emotionWord.length();

                        boolean testNegation = isNegated(token, previousToken1, previousToken2);
                        boolean testClimax = isClimaxed(token, emotionWord, tokenLength, emotionWordLength);

                        if (!testNegation && (testClimax || emotionWord.equals(token))) {
                            eed.addEmotionOccurrence(emotionWord, emotion, url, sentence);
                        }

                    }

                }
                previousToken2 = previousToken1;
                previousToken1 = token;
            }

        }

        LOGGER.debug(eed);
    }

    /**
     * 
     * @param word
     * @param word2
     * @param j
     * @param y
     * @param yy
     * @return
     */
    private boolean isClimaxed(String word, String word2, int y, int yy) {
        boolean testClimax = false;
        if (word.endsWith("er") || word.endsWith("en") || word.endsWith("es") || word.endsWith("em")) {
            String qs = word.substring(0, (y - 2));
            if (qs.equals(word2)) {
                testClimax = true;
            }
        } else {
            if (word.endsWith("e") || word.endsWith("s")) {
                String qs = word.substring(0, (y - 1));
                if (qs.equals(word2)) {
                    testClimax = true;
                }
            } else {
                if (word.endsWith("ern")) {
                    String qs = word.substring(0, (y - 3));
                    if (qs.equals(word2)) {
                        testClimax = true;
                    }
                } else {
                    testClimax = false;
                }
            }
        }
        return testClimax;
    }

    /**
     * Check whether a given word is negated.
     * 
     * @param word The word to check.
     * @param previousWord The word before the word.
     * @param previousWord2 The word two words before the word.
     * @return True if the word is negated.
     */
    private boolean isNegated(String word, String previousWord, String previousWord2) {
        boolean testNegation = true;
        if (("nicht".equals(previousWord2) == false) && ("nichts".equals(previousWord2) == false)
                && ("ohne".equals(previousWord) == false) && ("ohne".equals(previousWord2) == false)
                && ("keine".equals(previousWord) == false) && ("kein".equals(previousWord) == false)
                && ("nicht".equals(previousWord) == false)) {
            testNegation = false;
        }
        return testNegation;
    }

    @Override
    protected void saveExtractions(boolean saveExtractions) {
        // TODO Auto-generated method stub

    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        String url = "http://www.geizkragen.de/produkte/tablet-pc/apple-ipad-wi-fi-16gb-mb292fda-569426.html";
        EntityEmotionDistribution eed = new EntityEmotionDistribution("ipad");
        EmotionExtractor.getInstance().extractEmotions(eed, url);

    }

}
