package ws.palladian.extraction.token;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import ws.palladian.core.Token;
import ws.palladian.extraction.sentence.PalladianSentenceDetector;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;

/**
 * <p>
 * The Tokenizer tokenizes strings or creates chunks of that string.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class Tokenizer {

    /** The RegExp used for tokenization (terms). */
    public static final String TOKEN_SPLIT_REGEX = "(?:[A-Z]\\.)+|[\\p{L}\\w]+(?:[-\\.,][\\p{L}\\w]+)*|\\.[\\p{L}\\w]+|</?[\\p{L}\\w]+>|\\$\\d+\\.\\d+|[^\\w\\s<]+";

    /** The RegExp used for sentence splitting. */
    public static final String SENTENCE_SPLIT_REGEX_EN = "(?<!(\\.|\\()|([A-Z]\\.[A-Z]){1,10}|St|Mr|mr|Dr|dr|Prof|Mrs|mrs|Jr|jr|vs| eg|e\\.g|ca|etc| sq| ft)((\\.|\\?|\\!)(’|”|\")+(?=\\s+[A-Z])|\\.|\\?+|\\!+)(?!(\\.|[0-9]|\"|”|'|\\)|[!?]|(com|de|fr|uk|au|ca|cn|org|net)/?\\s|\\()|[A-Za-z]{1,15}\\.|[A-Za-z]{1,15}\\(\\))";
    public static final String SENTENCE_SPLIT_REGEX_DE = "(?<!(\\.|\\()|([A-Z]\\.[A-Z]){1,10}|St|[mM]r|[dD]r|Prof|[mM]s|[jJ]r|vs|ca|engl|evtl|etc| sog| ident|bzw|i\\.d\\.R|o\\.k|zzgl|bspw|bsp|m\\.E|bezügl|bzgl|inkl|exkl|ggf|z\\.\\s?[bB]| max| min|u\\.s\\.w|u\\.a|d\\.h)((\\.|\\?|\\!)(”|\")\\s[A-Z]|\\.|\\?+|\\!+)(?!(\\.|[0-9]|\"|”|'|\\)| B\\.|[!?]|(com|de|fr|uk|au|ca|cn|org|net)/?\\s|\\()|[A-Za-z]{1,15}\\.|[A-Za-z]{1,15}\\(\\))";

    private Tokenizer() {
        // prevent instantiation.
    }

    /**
     * <p>
     * Tokenize a given string.
     * </p>
     * 
     * @param inputString The string to be tokenized.
     * @return A list of tokens.
     */
    public static List<String> tokenize(String inputString) {
        Iterator<Token> tokenIterator = new WordTokenizer().iterateSpans(inputString);
        return CollectionHelper.newArrayList(CollectionHelper.convert(tokenIterator, Token.STRING_CONVERTER));
    }

    /**
     * <p>
     * Calculate n-grams for a given string on a character level. The size of the set can be calculated as: Size =
     * stringLength - n + 1.
     * </p>
     * 
     * @param string The string that the n-grams should be calculated for.
     * @param n The number of characters for a gram.
     * @return A set of n-grams.
     */
    public static Set<String> calculateCharNGrams(String string, int n) {
        Iterator<Token> nGramIterator = new CharacterNGramTokenizer(n, n).iterateSpans(string);
        return CollectionHelper.newHashSet(CollectionHelper.convert(nGramIterator, Token.STRING_CONVERTER));
    }

    /**
     * <p>
     * Calculate n-grams for a given string on a word level. The size of the set can be calculated as: Size =
     * numberOfWords - n + 1.
     * </p>
     * 
     * @param string The string that the n-grams should be calculated for.
     * @param n The number of words for a gram.
     * @return A set of n-grams.
     */
    public static Set<String> calculateWordNGrams(String string, int n) {
        return calculateAllWordNGrams(string, n, n);
    }

    /**
     * <p>
     * Calculate n-grams for a given string on a word level. The size of the set can be calculated as: Size =
     * numberOfWords - n + 1.
     * </p>
     * 
     * <p>
     * Since the quantity of the encountered n-grams is important for some algorithms, a list is used.
     * </p>
     * 
     * @param string The string that the n-grams should be calculated for.
     * @param n The number of words for a gram.
     * @return A list of n-grams.
     */
    public static List<String> calculateWordNGramsAsList(String string, int n) {
        Iterator<Token> tokenIterator = new WordTokenizer().iterateSpans(string);
        tokenIterator = new NGramWrapperIterator(tokenIterator, n, n);
        return CollectionHelper.newArrayList(CollectionHelper.convert(tokenIterator, Token.STRING_CONVERTER));
    }

    /**
     * <p>
     * Calculate all n-grams for a string for different n on a character level. The size of the set can be calculated
     * as: Size = SUM_n(n1,n2) (stringLength - n + 1)
     * </p>
     * 
     * @param string The string the n-grams should be calculated for.
     * @param n1 The smallest n-gram size.
     * @param n2 The greatest n-gram size.
     * @return A set of n-grams.
     */
    public static Set<String> calculateAllCharNGrams(String string, int n1, int n2) {
        Iterator<Token> tokenIterator = new CharacterNGramTokenizer(n1, n2).iterateSpans(string);
        return CollectionHelper.newHashSet(CollectionHelper.convert(tokenIterator, Token.STRING_CONVERTER));
    }

    /**
     * <p>
     * Calculate all n-grams for a string for different n on a word level. The size of the set can be calculated as:
     * Size = SUM_n(n1,n2) (numberOfWords - n + 1)
     * </p>
     * 
     * @param string The string the n-grams should be calculated for.
     * @param n1 The smallest n-gram size.
     * @param n2 The greatest n-gram size.
     * @return A set of n-grams.
     */
    public static Set<String> calculateAllWordNGrams(String string, int n1, int n2) {
        Iterator<Token> tokenIterator = new WordTokenizer().iterateSpans(string);
        tokenIterator = new NGramWrapperIterator(tokenIterator, n1, n2);
        return CollectionHelper.newHashSet(CollectionHelper.convert(tokenIterator, Token.STRING_CONVERTER));
    }

    public static String getSentence(String string, int position) {
        return getSentence(string, position, Language.ENGLISH);
    }

    /**
     * <p>
     * Get the sentence in which the specified position is present.
     * </p>
     * 
     * @param string The string.
     * @param position The position in the sentence.
     * @return The whole sentence.
     */
    private static String getSentence(String string, int position, Language language) {
        if (position < 0) {
            return string;
        }

        // /////// XXX
        List<String> sentences = getSentences(string, language);
        String pickedSentence = "";
        for (String sentence : sentences) {
            int start = string.indexOf(sentence);
            if (start <= position) {
                pickedSentence = sentence;
            } else {
                break;
            }
        }
        if (true) {
            return pickedSentence;
        }
        // ////////

        String beginning = getPhraseFromBeginningOfSentence(string.substring(0, position));
        String end = getPhraseToEndOfSentence(string.substring(position));
        if (beginning.endsWith(" ")) {
            end = end.trim();
        }

        return beginning + end;
    }

    public static List<String> getSentences(String inputText, boolean onlyRealSentences) {
        return getSentences(inputText, onlyRealSentences, Language.ENGLISH);
    }

    /**
     * <p>
     * Get a list of sentences of an input text. Also see <a
     * href="http://alias-i.com/lingpipe/demos/tutorial/sentences/read-me.html">http://alias-i.com/lingpipe/demos
     * /tutorial/sentences/read-me.html</a> for the LingPipe example.
     * </p>
     * 
     * @param inputText An input text.
     * @param onlyRealSentences If true, only sentences that end with a sentence delimiter are considered (headlines in
     *            texts will likely be discarded)
     * @param language The language to use for sentence splitting.
     * @return A list with sentences.
     */
    public static List<String> getSentences(String inputText, boolean onlyRealSentences, Language language) {
        List<Token> annotations = CollectionHelper.newArrayList(new PalladianSentenceDetector(language).iterateSpans(inputText));
        List<String> sentences = CollectionHelper.convertList(annotations, Token.STRING_CONVERTER);
        // TODO Since requirements might differ slightly from application to application, this filtering should be
        // carried out by each calling application itself.
        if (onlyRealSentences) {
            List<String> realSentences = CollectionHelper.newArrayList();
            for (String sentence : sentences) {
                String[] parts = sentence.split("\n");
                sentence = parts[parts.length - 1];
                if (sentence.endsWith(".") || sentence.endsWith("?") || sentence.endsWith("!")
                        || sentence.endsWith(".”") || sentence.endsWith(".\"")) {

                    String cleanSentence = StringHelper.trim(sentence, "“”\"");
                    int wordCount = StringHelper.countWhitespaces(cleanSentence) + 1;

                    // TODO Why is this 8?
                    // TODO There are valid english sentences with only one word like "Go!" or "Stop!"
                    if (cleanSentence.length() > 8 && wordCount > 2) {
                        realSentences.add(sentence.trim());
                    }
                }
            }
            sentences = realSentences;
        }
        return sentences;
    }

    /**
     * <p>
     * Splits a text into sentences.
     * </p>
     * 
     * @param inputText The text to split.
     * @return The senteces as they appear in the text.
     */
    public static List<String> getSentences(String inputText) {
        return getSentences(inputText, Language.ENGLISH);
    }

    public static List<String> getSentences(String inputText, Language language) {
        return getSentences(inputText, false, language);
    }

    /**
     * <p>
     * Given a string, find the beginning of the sentence, e.g. "...now. Although, many of them" =>
     * "Although, many of them". consider !,?,. and : as end of sentence.
     * </p>
     * TODO control character after delimiter makes it end of sentence.
     * 
     * @param inputString the input string
     * @return The phrase from the beginning of the sentence.
     */
    public static String getPhraseFromBeginningOfSentence(String inputString) {

        String string = inputString;
        string = StringHelper.removeDoubleWhitespaces(string);

        // find the beginning of the current sentence by finding the period at the end
        int startIndex = string.lastIndexOf(".");

        startIndex = Math.max(startIndex, string.lastIndexOf("\n"));

        // make sure point is not between numerals e.g. 30.2% (as this would not
        // be the end of the sentence, keep searching in this case)
        boolean pointIsSentenceDelimiter = false;
        while (!pointIsSentenceDelimiter && startIndex > -1) {
            if (startIndex >= string.length() - 1) {
                break;
            }

            if (startIndex > 0) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(startIndex - 1))
                        && Character.isUpperCase(string.charAt(startIndex + 1));
            }
            if (!pointIsSentenceDelimiter && startIndex < string.length() - 2) {
                pointIsSentenceDelimiter = (Character.isUpperCase(string.charAt(startIndex + 2))
                        || string.charAt(startIndex + 2) == '-' || string.charAt(startIndex + 2) == '=')
                        && string.charAt(startIndex + 1) == ' ';
            }

            // break after period
            if (!pointIsSentenceDelimiter
                    && (string.charAt(startIndex + 1) == '\n' || string.charAt(startIndex) == '\n')) {
                pointIsSentenceDelimiter = true;
            }

            if (pointIsSentenceDelimiter) {
                break;
            }

            if (startIndex < string.length() - 1) {
                startIndex = string.substring(0, startIndex).lastIndexOf(".");
            } else {
                startIndex = -1;
            }
        }

        if (string.lastIndexOf("!") > -1 && string.lastIndexOf("!") > startIndex) {
            startIndex = string.lastIndexOf("!");
        }

        if (string.lastIndexOf("?") > -1 && string.lastIndexOf("?") > startIndex) {
            startIndex = string.lastIndexOf("?");
        }

        if (string.lastIndexOf(":") > -1 && string.lastIndexOf(":") > startIndex) {
            startIndex = string.lastIndexOf(":");
        }

        if (startIndex == -1) {
            startIndex = -1;
        }

        // cut period
        string = string.substring(startIndex + 1);

        // cut first space
        if (string.startsWith(" ")) {
            string = string.substring(1);
        }

        return string;
    }

    /**
     * <p>
     * Given a string, find the end of the sentence, e.g. "Although, many of them (30.2%) are good. As long as" =>
     * "Although, many of them (30.2%) are good.". Consider !,?, and . as end of sentence.
     * </p>
     * 
     * @param string The string.
     * @return The phrase to the end of the sentence.
     */
    public static String getPhraseToEndOfSentence(String string) {

        // find the end of the current sentence
        int endIndex = string.indexOf(".");

        // make sure point is not between numerals e.g. 30.2% (as this would not
        // be the end of the sentence, keep searching in this case)
        // after point no number because 2 hr. 32 min. would be broken
        boolean pointIsSentenceDelimiter = false;
        while (!pointIsSentenceDelimiter && endIndex > -1) {

            // before period
            if (endIndex > 0) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(endIndex - 1));
            }
            // one digit after period
            if (endIndex < string.length() - 1) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(endIndex + 1))
                        && Character.isUpperCase(string.charAt(endIndex + 1))
                        || StringHelper.isBracket(string.charAt(endIndex + 1))
                        || (endIndex > 0 && string.charAt(endIndex - 1) == '"');
            }
            // two digits after period
            if (!pointIsSentenceDelimiter && endIndex < string.length() - 2) {
                pointIsSentenceDelimiter = !StringHelper.isNumber(string.charAt(endIndex + 2))
                        && (Character.isUpperCase(string.charAt(endIndex + 2)) || StringHelper.isBracket(string
                                .charAt(endIndex + 2))) && string.charAt(endIndex + 1) == ' ';
            }
            // break after period
            if (!pointIsSentenceDelimiter && (string.length() == (endIndex + 1) || string.charAt(endIndex + 1) == '\n')) {
                pointIsSentenceDelimiter = true;
            }

            if (pointIsSentenceDelimiter) {
                break;
            }

            if (endIndex < string.length() - 1) {
                endIndex = string.indexOf(".", endIndex + 1);
            } else {
                endIndex = -1;
            }
        }

        if (string.indexOf("!") > -1 && (string.indexOf("!") < endIndex || endIndex == -1)) {
            endIndex = string.indexOf("!");
        }

        if (string.indexOf("?") > -1 && (string.indexOf("?") < endIndex || endIndex == -1)) {
            endIndex = string.indexOf("?");
        }

        // XXX commented this out because of aspect ratio "2.35 : 1" wasn't captured
        // if (string.indexOf(":") > -1 && (string.indexOf(":") < endIndex || endIndex == -1)) {
        // int indexColon = string.indexOf(":");
        // if (string.length() > indexColon + 1 && !StringHelper.isNumber(string.charAt(indexColon + 1))) {
        // endIndex = indexColon;
        // }
        //
        // }
        if (endIndex == -1) {
            endIndex = string.length();
        }

        else {
            ++endIndex; // take last character as well
        }

        return string.substring(0, endIndex);
    }

    public static void main(String[] args) throws IOException {

        StopWatch stopWatch = new StopWatch();

        for (int i = 0; i < 1000; i++) {
            Tokenizer
            .getSentences("Zum Einen ist das Ding ein bisschen groß und es sieht sehr merkwürdig aus, wenn man damit durch die Stadt läuft und es am Ohr hat und zum Anderen ein bisschen unhandlich.\nNun möchte ich noch etwas über die Akkulaufzeit sagen.");
        }
        System.out.println(stopWatch.getElapsedTimeString());

        // System.out.println(Tokenizer.tokenize("schön"));
        // System.out.println(Tokenizer.tokenize("web2.0 web 2.0 .net asp.net test-test 30,000 people"));
        System.exit(0);

        System.out.println(getSentences("the quick brown fox"));

        // demo for the tokenizer problem
        String text = FileHelper.readFileToString("data/test/tokenizerProblem.txt");

        // tokenize the whole text
        int count = 0;
        List<String> tokens = Tokenizer.tokenize(text);
        for (String token : tokens) {
            if (token.equals("Number")) {
                count++;
            }
        }
        System.out.println("# occurences 1 : " + count);

        // split text into sentences,
        // then tokenize each sentence
        count = 0;
        List<String> sentences = Tokenizer.getSentences(text);

        for (String sentence : sentences) {
            FileHelper.appendFile("sentences.txt", sentence + "\n");
            List<String> tokensInSentence = Tokenizer.tokenize(sentence);
            for (String token : tokensInSentence) {
                if (token.equals("Number")) {
                    count++;
                }
            }
        }
        System.out.println("# occurences 2 : " + count);
    }
}
