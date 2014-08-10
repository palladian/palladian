package ws.palladian.extraction.sentence;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.entity.Annotations;
import ws.palladian.extraction.entity.DateAndTimeTagger;
import ws.palladian.extraction.entity.SmileyTagger;
import ws.palladian.extraction.entity.UrlTagger;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.DateFormat;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.nlp.StringHelper;

public final class PalladianSentenceDetector extends AbstractSentenceDetector {

    private static final DateFormat[] ALL_DATES_WITH_DOTS = new DateFormat[] {RegExp.DATE_EU_D_MM,
        RegExp.DATE_EU_D_MM_Y, RegExp.DATE_EU_D_MM_Y_T, RegExp.DATE_EU_D_MMMM, RegExp.DATE_EU_D_MMMM_Y,
        RegExp.DATE_EU_D_MMMM_Y_T, RegExp.DATE_EU_MM_Y, RegExp.DATE_USA_MMMM_D_Y, RegExp.DATE_USA_MMMM_D_Y_SEP,
        RegExp.DATE_USA_MMMM_D_Y_T, RegExp.DATE_USA_MMMM_D, RegExp.DATE_EUSA_MMMM_Y, RegExp.DATE_EUSA_YYYY_MMM_D};

    private static final UrlTagger URL_TAGGER = new UrlTagger();
    private static final DateAndTimeTagger DATE_TIME_TAGGER = new DateAndTimeTagger(ALL_DATES_WITH_DOTS);
    private static final SmileyTagger SMILEY_TAGGER = new SmileyTagger();
    
    private final Language language;

    public PalladianSentenceDetector(Language language) {
        Validate.notNull(language, "language must not be null");
        this.language = language;
    }

    @Override
    public List<Annotation> getAnnotations(String text) {
        Pattern pattern = Tokenizer.SENTENCE_SPLIT_PATTERN_EN;
        if (language == Language.GERMAN) {
            pattern = Tokenizer.SENTENCE_SPLIT_PATTERN_DE;
        }
        return getSentences(text, pattern);
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

            String transformedValue = String.valueOf(text.subSequence(originalStartPosition, originalEndPosition));
            Annotation transformedSentence = new ImmutableAnnotation(originalStartPosition, transformedValue,
                    StringUtils.EMPTY);
            ret.add(transformedSentence);
            lastOriginalEndPosition = originalEndPosition;
            lastEndPosition = sentence.getEndPosition();
        }

        return ret;
    }

//    /**
//     * <p>
//     * Splits the text of {@code inputDocument} into sentences. The text should be in the language provided as parameter
//     * {@code language}.
//     * </p>
//     * 
//     * @param inputDocument The {@link TextDocument} to split into sentences.
//     * @param featureName The name of the created {@link Annotation}s.
//     * @param language The language of the text to split into sentences.
//     * @return A {@link List} of {@link Annotation}s marking the sentences the text was split into.
//     */
//    public static List<Annotation> getAnnotatedSentences(String text, Language language) {
//        Pattern pattern = Tokenizer.SENTENCE_SPLIT_PATTERN_EN;
//        if (language == Language.GERMAN) {
//            pattern = Tokenizer.SENTENCE_SPLIT_PATTERN_DE;
//        }
//        return getSentences(text, pattern);
//    }

}