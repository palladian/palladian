package ws.palladian.extraction.token;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.classification.text.CharacterNGramIterator;
import ws.palladian.classification.text.NGramWrapperIterator;
import ws.palladian.classification.text.TokenIterator;
import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.DateAndTimeTagger;
import ws.palladian.extraction.entity.SmileyTagger;
import ws.palladian.extraction.entity.UrlTagger;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.DateFormat;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.constants.RegExp;
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

    private static final Pattern SENTENCE_SPLIT_PATTERN_EN = Pattern.compile(SENTENCE_SPLIT_REGEX_EN);
    private static final Pattern SENTENCE_SPLIT_PATTERN_DE = Pattern.compile(SENTENCE_SPLIT_REGEX_DE);

    /** The compiled pattern used for tokenization, using {@link Tokenizer#TOKEN_SPLIT_REGEX}. */
    public static final Pattern SPLIT_PATTERN = Pattern.compile(TOKEN_SPLIT_REGEX, Pattern.DOTALL
            | Pattern.CASE_INSENSITIVE);

    private static final DateFormat[] ALL_DATES_WITH_DOTS = new DateFormat[] {RegExp.DATE_EU_D_MM,
        RegExp.DATE_EU_D_MM_Y, RegExp.DATE_EU_D_MM_Y_T, RegExp.DATE_EU_D_MMMM, RegExp.DATE_EU_D_MMMM_Y,
        RegExp.DATE_EU_D_MMMM_Y_T, RegExp.DATE_EU_MM_Y, RegExp.DATE_USA_MMMM_D_Y, RegExp.DATE_USA_MMMM_D_Y_SEP,
        RegExp.DATE_USA_MMMM_D_Y_T, RegExp.DATE_USA_MMMM_D, RegExp.DATE_EUSA_MMMM_Y, RegExp.DATE_EUSA_YYYY_MMM_D};

    private static final UrlTagger URL_TAGGER = new UrlTagger();
    private static final DateAndTimeTagger DATE_TIME_TAGGER = new DateAndTimeTagger(ALL_DATES_WITH_DOTS);
    private static final SmileyTagger SMILEY_TAGGER = new SmileyTagger();

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
        TokenIterator tokenIterator = new TokenIterator(inputString);
        return CollectionHelper.newArrayList(tokenIterator);
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
        Iterator<String> nGramIterator = new CharacterNGramIterator(string, n, n);
        return CollectionHelper.newHashSet(nGramIterator);
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
        Iterator<String> tokenIterator = new TokenIterator(string);
        tokenIterator = new NGramWrapperIterator(tokenIterator, n, n);
        return CollectionHelper.newArrayList(tokenIterator);
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
        Iterator<String> tokenIterator = new CharacterNGramIterator(string, n1, n2);
        return CollectionHelper.newHashSet(tokenIterator);
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
        Iterator<String> tokenIterator = new TokenIterator(string);
        tokenIterator = new NGramWrapperIterator(tokenIterator, n1, n2);
        return CollectionHelper.newHashSet(tokenIterator);
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

    public static List<String> getSentences(String inputText, boolean onlyRealSentences, Language language) {
        Pattern pattern = SENTENCE_SPLIT_PATTERN_EN;
        if (language == Language.GERMAN) {
            pattern = SENTENCE_SPLIT_PATTERN_DE;
        }
        return getSentences(inputText, onlyRealSentences, pattern);
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
        List<Annotation> taggedUrls = URL_TAGGER.getAnnotations(inputText);
        int uCount = 1;
        Map<String, String> urlMapping = new HashMap<String, String>();
        for (Annotation annotation : taggedUrls) {
            String replacement = "URL" + uCount;
            inputText = inputText.replace(annotation.getValue(), replacement);
            urlMapping.put(replacement, annotation.getValue());
            uCount++;
        }

        // recognize dates so we don't break them
        List<Annotation> taggedDates = DATE_TIME_TAGGER.getAnnotations(inputText);
        int dCount = 1;
        Map<String, String> dateMapping = new HashMap<String, String>();
        for (Annotation annotation : taggedDates) {
            String replacement = "DATE" + dCount;
            inputText = inputText.replace(annotation.getValue(), replacement);
            dateMapping.put(replacement, annotation.getValue());
            dCount++;
        }

        // recognize smileys so we don't break them
        List<Annotation> taggedSmileys = SMILEY_TAGGER.getAnnotations(inputText);
        int sCount = 1;
        Map<String, String> smileyMapping = new HashMap<String, String>();
        for (Annotation annotation : taggedSmileys) {
            String replacement = "SMILEY" + sCount;
            inputText = inputText.replace(annotation.getValue(), replacement);
            smileyMapping.put(replacement, annotation.getValue());
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

        // TODO Since requirements might differ slightly from application to application, this filtering should be
        // carried out by each calling application itself.
        if (onlyRealSentences) {

            List<String> realSentences = new ArrayList<String>();
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

    /**
     * <p>
     * Replaces all of the provided {@link Annotations} with mask in the {@code document}. The masking annotations are
     * added to the {@code annotationsForMaskedText} while the text containing the masks is created based on
     * {@code maskedText} and is returned.
     * </p>
     * 
     * @param document The {@link TextDocument} containing the original text.
     * @param annotations The {@link Annotations} to search for in the text.
     * @param maskedText The text to add the masks to.
     * @param mask The mask to add. This should be something that will never occur within the text itself.
     * @return The {@code maskedText} with the additional masks added during this run of the method.
     */
    private static String maskAnnotations(String text, List<Annotation> annotations, String mask,
            List<Annotation> annotationsForMaskedText, String maskedText) {
        for (Annotation annotation : annotations) {
            // This check is necessary to handle nested masks. Such masks are not replaced in the text and should not be
            // added to the list of masks.
            if (maskedText.contains(annotation.getValue())) {
                maskedText = StringUtils.replaceOnce(maskedText, annotation.getValue(), mask);
                annotationsForMaskedText.add(annotation);
            }
        }

        return maskedText;
    }

    /**
     * <p>
     * Splits the text of {@code inputDocument} into sentences. The text should be in the language provided as parameter
     * {@code language}.
     * </p>
     * 
     * @param inputDocument The {@link TextDocument} to split into sentences.
     * @param featureName The name of the created {@link Annotation}s.
     * @param language The language of the text to split into sentences.
     * @return A {@link List} of {@link Annotation}s marking the sentences the text was split into.
     */
    public static List<Annotation> getAnnotatedSentences(String text, Language language) {
        Pattern pattern = SENTENCE_SPLIT_PATTERN_EN;
        if (language == Language.GERMAN) {
            pattern = SENTENCE_SPLIT_PATTERN_DE;
        }
        return getSentences(text, pattern);
    }

    // TODO Add recognition of Java Stack Traces as they occur quite often in technical texts and are recognized as a
    // mixture of URLs and several sentence at the moment.
    /**
     * <p>
     * Splits the text of {@code inputDocument} into sentences using the provided regular expression.
     * </p>
     * 
     * @param inputDocument The {@link TextDocument} to split into sentences.
     * @param pattern The {@link Pattern} to use to split sentences.
     * @return A {@link List} of {@link Annotation}s marking the sentences the text was split into.
     */
    private static List<Annotation> getSentences(String text, Pattern pattern) {
        String inputText = text;
        String mask = "PALLADIANMASK";
        List<Annotation> masks = CollectionHelper.newArrayList();
        String maskedText = text;

        // recognize URLs so we don't break them
        List<Annotation> taggedUrlsAnnotations = URL_TAGGER.getAnnotations(inputText);
        maskedText = maskAnnotations(text, taggedUrlsAnnotations, mask, masks, maskedText);

        // recognize dates so we don't break them
        List<Annotation> taggedDates = DATE_TIME_TAGGER.getAnnotations(inputText);
        maskedText = maskAnnotations(text, taggedDates, mask, masks, maskedText);

        // recognize smileys so we don't break them
        List<Annotation> taggedSmileys = SMILEY_TAGGER.getAnnotations(inputText);
        maskedText = maskAnnotations(text, taggedSmileys, mask, masks, maskedText);

        List<Annotation> sentences = CollectionHelper.newArrayList();

        // pattern to find the end of a sentence
        Matcher matcher = pattern.matcher(maskedText);
        int lastIndex = 0;

        while (matcher.find()) {
            int endPosition = matcher.end();

            String untrimmedValue = maskedText.substring(lastIndex, endPosition);
            String leftTrimmedValue = StringHelper.ltrim(untrimmedValue);
            Integer leftOffset = untrimmedValue.length() - leftTrimmedValue.length();
            String value = StringHelper.rtrim(leftTrimmedValue);

            int leftIndex = lastIndex + leftOffset;
            Annotation sentence = new ImmutableAnnotation(leftIndex, value, StringUtils.EMPTY);
            sentences.add(sentence);
            lastIndex = endPosition;
        }

        // if we could not tokenize the whole string, which happens when the text was not terminated by a punctuation
        // character, just add the last fragment
        if (lastIndex < maskedText.length()) {
            // the following code is necessary to know how many characters are trimmed from the left and from the right
            // of the remaining content.
            String untrimmedValue = maskedText.substring(lastIndex);
            String leftTrimmedValue = StringHelper.ltrim(untrimmedValue);
            Integer leftOffset = untrimmedValue.length() - leftTrimmedValue.length();
            String value = StringHelper.rtrim(leftTrimmedValue);
            // Since there often is a line break at the end of a file this should not be added here.
            if (!value.isEmpty()) {
                int leftIndex = lastIndex + leftOffset;
                Annotation lastSentenceAnnotation = new ImmutableAnnotation(leftIndex, value, StringUtils.EMPTY);
                sentences.add(lastSentenceAnnotation);
            }
        }

        // replace masks back
        Collections.sort(masks, new Comparator<Annotation>() {

            @Override
            public int compare(Annotation o1, Annotation o2) {
                return Integer.valueOf(o1.getStartPosition()).compareTo(o2.getStartPosition());
            }
        });
        return recalculatePositions(text, maskedText, masks, sentences);
    }

    /**
     * <p>
     * Remapps the start and end position of all sentences from a masked text to the true text of the
     * {@code inputDocument}.
     * </p>
     * 
     * @param inputDocument The {@link TextDocument} containing the original text.
     * @param maskedText The text where dates, urls and smileys are masked so they do not break sentence splitting.
     * @param maskAnnotations A list of masked {@link Annotation}s that must be sorted by start position.
     * @param sentences The extracted sentences on {@code maskedText}, which should be remapped to the text of the
     *            {@code inputDocument}.
     * @param featureName The name of the created {@link Annotation}s.
     */
    private static List<Annotation> recalculatePositions(String text, String maskedText,
            List<Annotation> maskAnnotations, List<Annotation> sentences) {
        List<Annotation> ret = CollectionHelper.newArrayList();

        int lastOriginalEndPosition = 0;
        int lastEndPosition = 0;
        String mask = "PALLADIANMASK";
        Pattern maskPattern = Pattern.compile(mask);
        int maskLength = mask.length();
        int maskAnnotationIndex = 0;
        for (Annotation sentence : sentences) {
            // The space between this and the last sentence is this sentences start position in the transformed text -
            // the last sentences end position in the transformed text.
            int spaceBetweenSentences = sentence.getStartPosition() - lastEndPosition;
            // The start position of this sentence in the original text is the end position of the last sentence in the
            // original text + the space between both sentences.
            int originalStartPosition = lastOriginalEndPosition + spaceBetweenSentences;
            // The current offset, which needs to be added to all numbers in the transformed sentence to get their
            // original value is the start position in the original text - the sentences start position in the
            // transformed text.
            int currentOffset = originalStartPosition - sentence.getStartPosition();
            // The temporal end position of this sentence in the original text is calculated by adding the offset to the
            // end position in the transformed text. This is of course only true if their are no PALLADIANMASK elements
            // in the text. Those are added below.
            int originalEndPosition = sentence.getEndPosition() + currentOffset;

            // Search sentences for PALLADIANMASK
            Matcher maskMatcher = maskPattern.matcher(sentence.getValue());
            while (maskMatcher.find()) {
                Annotation maskAnnotation = maskAnnotations.get(maskAnnotationIndex);
                originalEndPosition += maskAnnotation.getValue().length() - maskLength;
                maskAnnotationIndex++;
                // handle contained masks by jumping over them
                // while (maskAnnotationIndex < maskAnnotations.size()
                // && maskAnnotations.get(maskAnnotationIndex).getEndPosition() <= maskAnnotation.getEndPosition()) {
                // maskAnnotationIndex++;
                // }
            }

            String transformedValue = String.valueOf(text.subSequence(originalStartPosition,
                    originalEndPosition));
            Annotation transformedSentence = new ImmutableAnnotation(originalStartPosition,transformedValue,StringUtils.EMPTY);
            ret.add(transformedSentence);
            lastOriginalEndPosition = originalEndPosition;
            lastEndPosition = sentence.getEndPosition();
        }

        return ret;
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
