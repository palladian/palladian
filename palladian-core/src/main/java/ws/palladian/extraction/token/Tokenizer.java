package ws.palladian.extraction.token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ws.palladian.extraction.entity.Annotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.DateAndTimeTagger;
import ws.palladian.extraction.entity.SmileyTagger;
import ws.palladian.extraction.entity.UrlTagger;
import ws.palladian.helper.StopWatch;
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
    public static final String SENTENCE_SPLIT_REGEX = "(?<!(\\.|\\()|([A-Z]\\.[A-Z]){1,10}|St|Mr|mr|Dr|dr|Prof|Mrs|mrs|Jr|jr|vs|ca|etc)(\\.|\\?+|\\!+)(?!(\\.|[0-9]|\"|'|\\)|[!?]|(com|de|fr|uk|au|ca|cn|org|net)/?\\s|\\()|[A-Za-z]{1,15}\\.|[A-Za-z]{1,15}\\(\\))";

    private static final Pattern SENTENCE_SPLIT_PATTERN = Pattern.compile(SENTENCE_SPLIT_REGEX);

    /** The compiled pattern used for tokenization, using {@link Tokenizer#TOKEN_SPLIT_REGEX}. */
    public static final Pattern SPLIT_PATTERN = Pattern.compile(TOKEN_SPLIT_REGEX, Pattern.DOTALL
            | Pattern.CASE_INSENSITIVE);

    private static UrlTagger urlTagger = new UrlTagger();
    private static DateAndTimeTagger dateAndTimeTagger = new DateAndTimeTagger();
    private static SmileyTagger smileyTagger = new SmileyTagger();

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

        List<String> tokens = new ArrayList<String>();

        Matcher matcher = SPLIT_PATTERN.matcher(inputString);
        while (matcher.find()) {
            tokens.add(matcher.group(0));
        }

        return tokens;
    }

    /**
     * <p>
     * Calculate all spans for a given string.
     * </p>
     * <p>
     * For example, the string "a b c" will return 7 spans (2^3=8 but all empty is not allowed, hence 7):
     * 
     * <pre>
     * a b c
     * a b
     * a c
     * b c
     * c
     * b
     * a
     * </pre>
     * 
     * </p>
     * 
     * @param string A tokenized string to get the spans for.
     * @param lengthThreshold The maximum length for extracted spans. For the above example set this to 3 to get all
     *            spans or to a smaller value to get only spans of that length or smaller. If the value is larger than
     *            the amount of tokens in {@code string} all spans are returned, if it is smaller than 1 all patterns of
     *            length 1 will be returned nevertheless.
     * @return A collection of spans.
     */
    public static Collection<List<String>> getAllSpans(String[] tokens, Integer lengthThreshold) {

        // create bitvector (all bit combinations other than all zeros)
        int bits = tokens.length;
        List<List<String>> spans = new ArrayList<List<String>>();

        int max = (int)Math.pow(2, bits);
        for (long i = 1; i < max; i++) {
            List<String> span = new LinkedList<String>();
            if (extractSpanRecursive(i, tokens, span, 0, Math.max(lengthThreshold - 1, 0))) {
                spans.add(span);
            }
        }

        return spans;
    }

    /**
     * <p>
     * Recursive extraction function for text spans.
     * </p>
     * 
     * @param bitPattern The pattern describing the indices in the list of {@code tokens} to include in the resulting
     *            span.
     * @param tokens The list of tokens to construct spans from.
     * @param span The result span will be constructed into this list.
     * @param currentIndex The current index in the list of tokens. For this call the algorithm needs to decide whether
     *            to include the token at that position in the span or not based on whether the value in
     *            {@code bitPattern} module 2 is 1 ({@code true}) or 0 ({@code false}).
     * @param maxSpanLength The maximum length for extracted spans. All spans beyond that length will cause the function
     *            to abort processing and return {@code false}.
     * @return {@code true} if the extracted span is smaller or equal to {@code maxSpanLength}; {@code false} otherwise.
     */
    private static Boolean extractSpanRecursive(Long bitPattern, String[] tokens, List<String> span,
            Integer currentIndex, Integer maxSpanLength) {
        if (bitPattern % 2 != 0) {
            span.add(tokens[currentIndex]);
        }
        Long nextBitPattern = bitPattern / 2;
        if (nextBitPattern < 1) {
            return true;
        } else if (span.size() > maxSpanLength) {
            return false;
        } else {
            return extractSpanRecursive(nextBitPattern, tokens, span, ++currentIndex, maxSpanLength);
        }
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
        Set<String> nGrams = new HashSet<String>();

        if (string.length() < n) {
            return nGrams;
        }

        for (int i = 0; i <= string.length() - n; i++) {

            StringBuilder nGram = new StringBuilder();
            for (int j = i; j < i + n; j++) {
                nGram.append(string.charAt(j));
            }
            nGrams.add(nGram.toString());

        }

        return nGrams;
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
        Set<String> nGrams = new HashSet<String>();

        String[] words = string.split("\\s");

        if (words.length < n) {
            return nGrams;
        }

        for (int i = 0; i <= words.length - n; i++) {

            StringBuilder nGram = new StringBuilder();
            for (int j = i; j < i + n; j++) {
                nGram.append(words[j]).append(" ");
            }
            nGrams.add(nGram.toString().trim());

        }

        return nGrams;
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
        List<String> nGrams = new ArrayList<String>();

        String[] words = string.split("\\s");

        if (words.length < n) {
            return nGrams;
        }

        for (int i = 0; i <= words.length - n; i++) {

            StringBuilder nGram = new StringBuilder();
            for (int j = i; j < i + n; j++) {
                nGram.append(words[j]).append(" ");
            }
            nGrams.add(nGram.toString().trim());

        }

        return nGrams;
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
        Set<String> nGrams = new HashSet<String>();
        for (int n = n1; n <= n2; n++) {
            nGrams.addAll(calculateCharNGrams(string, n));
        }

        return nGrams;
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
        Set<String> nGrams = new HashSet<String>();
        for (int n = n1; n <= n2; n++) {
            nGrams.addAll(calculateWordNGrams(string, n));
        }

        return nGrams;
    }

    /**
     * <p>
     * Calculates all n-grams for n ranging from {@code minSize} included up to {@code maxSize} included over a list of
     * tokens.
     * </p>
     * 
     * @param token The tokens to n-grammize.
     * @param minSize The lower bound for the size of the extracted n-grams.
     * @param maxSize The upper bound for the size of the extracted n-grams.
     * @return A {@code List} of n-grams wich are represented as a {@code List} of tokens each.
     */
    public static List<List<String>> calculateAllNGrams(String[] token, Integer minSize, Integer maxSize) {
        List<List<String>> ret = new ArrayList<List<String>>();

        for (int n = minSize; n <= maxSize; n++) {
            ret.addAll(calculateNGrams(token, n));
        }

        return ret;
    }

    /**
     * <p>
     * Calculates n-grams of a certain size for an array of token.
     * </p>
     * 
     * @param token The token to n-grammize.
     * @param size The size of the desired n-grams.
     * @return A {@code List} of n-grams which are represented as a {@code List} of tokens each.
     */
    public static List<List<String>> calculateNGrams(String[] token, Integer size) {
        List<List<String>> nGrams = new ArrayList<List<String>>();

        if (token.length < size) {
            return nGrams;
        }

        for (int i = 0; i <= token.length - size; i++) {

            List<String> nGram = new ArrayList<String>(size);
            for (int j = i; j < i + size; j++) {
                nGram.add(token[j]);
            }
            nGrams.add(nGram);

        }

        return nGrams;
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
    public static String getSentence(String string, int position) {
        if (position < 0) {
            return string;
        }

        String beginning = getPhraseFromBeginningOfSentence(string.substring(0, position));
        String end = getPhraseToEndOfSentence(string.substring(position));
        if (beginning.endsWith(" ")) {
            end = end.trim();
        }

        return beginning + end;
    }

    public static List<String> getSentences(String inputText, boolean onlyRealSentences) {
        return getSentences(inputText, onlyRealSentences, SENTENCE_SPLIT_PATTERN);
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
     * @param The pattern to use for sentence splitting.
     * @return A list with sentences.
     */
    public static List<String> getSentences(String inputText, boolean onlyRealSentences, Pattern pattern) {

        // recognize URLs so we don't break them
        Annotations taggedUrls = urlTagger.tagUrls(inputText);
        int uCount = 1;
        Map<String, String> urlMapping = new HashMap<String, String>();
        for (Annotation annotation : taggedUrls) {
            String replacement = "URL" + uCount;
            inputText = inputText.replace(annotation.getEntity(), replacement);
            urlMapping.put(replacement, annotation.getEntity());
            uCount++;
        }

        // recognize dates so we don't break them
        Annotations taggedDates = dateAndTimeTagger.tagDateAndTime(inputText);
        int dCount = 1;
        Map<String, String> dateMapping = new HashMap<String, String>();
        for (Annotation annotation : taggedDates) {
            String replacement = "DATE" + dCount;
            inputText = inputText.replace(annotation.getEntity(), replacement);
            dateMapping.put(replacement, annotation.getEntity());
            dCount++;
        }

        // recognize smileys so we don't break them
        Annotations taggedSmileys = smileyTagger.tagSmileys(inputText);
        int sCount = 1;
        Map<String, String> smileyMapping = new HashMap<String, String>();
        for (Annotation annotation : taggedSmileys) {
            String replacement = "SMILEY" + sCount;
            inputText = inputText.replace(annotation.getEntity(), replacement);
            smileyMapping.put(replacement, annotation.getEntity());
            sCount++;
        }

        List<String> sentences = new ArrayList<String>();

        // pattern to find the end of a sentence
        Matcher matcher = pattern.matcher(inputText);
        int lastIndex = 0;

        while (matcher.find()) {
            sentences.add(inputText.substring(lastIndex, matcher.end()).trim());
            lastIndex = matcher.end();
        }

        // if we could not tokenize the whole string, which happens when the text was not terminated by a punctuation
        // character, just add the last fragment
        if (lastIndex < inputText.length()) {
            sentences.add(inputText.substring(lastIndex).trim());
        }

        if (onlyRealSentences) {

            List<String> realSentences = new ArrayList<String>();
            for (String sentence : sentences) {
                String[] parts = sentence.split("\n");
                sentence = parts[parts.length - 1];
                if (sentence.endsWith(".") || sentence.endsWith("?") || sentence.endsWith("!")) {

                    String cleanSentence = StringHelper.trim(sentence);
                    int wordCount = StringHelper.countWhitespaces(cleanSentence) + 1;

                    if (cleanSentence.length() > 8 && wordCount > 2) {
                        realSentences.add(sentence.trim());
                    }
                }
            }

            sentences = realSentences;
        }

        // replace URLs back
        List<String> sentencesReplacedUrls = new ArrayList<String>();
        for (String sentence : sentences) {
            for (Entry<String, String> entry : urlMapping.entrySet()) {
                sentence = sentence.replace(entry.getKey(), entry.getValue());
            }
            sentencesReplacedUrls.add(sentence);
        }

        // replace dates back
        List<String> sentencesReplacedDates = new ArrayList<String>();
        for (String sentence : sentencesReplacedUrls) {
            for (Entry<String, String> entry : dateMapping.entrySet()) {
                sentence = sentence.replace(entry.getKey(), entry.getValue());
            }
            if (!sentence.isEmpty()) {
                sentencesReplacedDates.add(sentence);
            }
        }

        // replace smileys back
        List<String> sentencesReplacedSmileys = new ArrayList<String>();
        for (String sentence : sentencesReplacedDates) {
            for (Entry<String, String> entry : smileyMapping.entrySet()) {
                sentence = sentence.replace(entry.getKey(), entry.getValue());
            }
            if (!sentence.isEmpty()) {
                sentencesReplacedSmileys.add(sentence);
            }
        }

        return sentencesReplacedSmileys;
    }

    public static List<String> getSentences(String inputText) {
        return getSentences(inputText, false);
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

        // find the beginning of the current sentence by finding the period at the end
        int startIndex = string.lastIndexOf(".");

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
                pointIsSentenceDelimiter = (Character.isUpperCase(string.charAt(startIndex + 2)) || string
                        .charAt(startIndex + 2) == '-') && string.charAt(startIndex + 1) == ' ';
            }
            // break after period
            if (!pointIsSentenceDelimiter && string.charAt(startIndex + 1) == '\n') {
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
                        || StringHelper.isBracket(string.charAt(endIndex + 1));
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
