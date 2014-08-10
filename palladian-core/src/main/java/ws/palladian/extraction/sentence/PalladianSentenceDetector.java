package ws.palladian.extraction.sentence;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import ws.palladian.core.Annotation;
import ws.palladian.core.ImmutableAnnotation;
import ws.palladian.extraction.entity.DateAndTimeTagger;
import ws.palladian.extraction.entity.SmileyTagger;
import ws.palladian.extraction.entity.UrlTagger;
import ws.palladian.extraction.token.Tokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.DateFormat;
import ws.palladian.helper.constants.Language;
import ws.palladian.helper.constants.RegExp;
import ws.palladian.helper.nlp.StringHelper;

/**
 * Palladian's sentence detector. Sentences are detected using regular expressions and by recognizing URLs, Emoticons
 * and Dates thus avoiding to break at those positions.
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class PalladianSentenceDetector extends AbstractSentenceDetector {

    /** Sentence split pattern for English text. */
    private static final Pattern PATTERN_EN = Pattern.compile(Tokenizer.SENTENCE_SPLIT_REGEX_EN);

    /** Sentence split pattern for German text. */
    private static final Pattern PATTERN_DE = Pattern.compile(Tokenizer.SENTENCE_SPLIT_REGEX_DE);

    /** All date formats which include dots. */
    private static final DateFormat[] DATES_WITH_DOTS = new DateFormat[] {RegExp.DATE_EU_D_MM, //
            RegExp.DATE_EU_D_MM_Y, //
            RegExp.DATE_EU_D_MM_Y_T, //
            RegExp.DATE_EU_D_MMMM, //
            RegExp.DATE_EU_D_MMMM_Y,//
            RegExp.DATE_EU_D_MMMM_Y_T, //
            RegExp.DATE_EU_MM_Y, //
            RegExp.DATE_USA_MMMM_D_Y,//
            RegExp.DATE_USA_MMMM_D_Y_SEP,//
            RegExp.DATE_USA_MMMM_D_Y_T,//
            RegExp.DATE_USA_MMMM_D, //
            RegExp.DATE_EUSA_MMMM_Y, //
            RegExp.DATE_EUSA_YYYY_MMM_D};

    /** Tagger for the date formats. */
    private static final DateAndTimeTagger DATE_TAGGER = new DateAndTimeTagger(DATES_WITH_DOTS);

    /** Character which is used as replacement recognized entities (URLs, Dates, Emoticons). */
    private static final char MASK_CHARACTER = 'M';

    /** The language to use for sentence splitting. */
    private final Language language;

    public PalladianSentenceDetector(Language language) {
        Validate.notNull(language, "language must not be null");
        this.language = language;
    }

    @Override
    public List<Annotation> getAnnotations(String text) {
        // recognize URLs, dates and smileys, so we don't break them
        List<Annotation> maskAnnotations = CollectionHelper.newArrayList();
        maskAnnotations.addAll(UrlTagger.INSTANCE.getAnnotations(text));
        maskAnnotations.addAll(DATE_TAGGER.getAnnotations(text));
        maskAnnotations.addAll(SmileyTagger.INSTANCE.getAnnotations(text));

        // replace recognized entities with mask placeholder
        StringBuilder maskedTextBuilder = new StringBuilder(text);
        for (Annotation annotation : maskAnnotations) {
            String replacement = StringUtils.repeat(MASK_CHARACTER, annotation.getValue().length());
            maskedTextBuilder.replace(annotation.getStartPosition(), annotation.getEndPosition(), replacement);
        }
        String maskedText = maskedTextBuilder.toString();

        // tokenize the masked text
        List<Annotation> maskedSentences = CollectionHelper.newArrayList();
        Pattern pattern = language == Language.GERMAN ? PATTERN_DE : PATTERN_EN;
        Matcher matcher = pattern.matcher(maskedText);
        int lastIndex = 0;
        while (matcher.find()) {
            int endPosition = matcher.end();
            Annotation annotation = createAnnotation(maskedText, lastIndex, endPosition);
            if (annotation != null) {
                maskedSentences.add(annotation);
            }
            lastIndex = endPosition;
        }
        // add last fragment, in case we could not tokenize the whole string
        if (lastIndex < maskedText.length()) {
            Annotation annotation = createAnnotation(maskedText, lastIndex, maskedText.length());
            if (annotation != null) {
                maskedSentences.add(annotation);
            }
        }

        // recreate annotations without masks
        List<Annotation> sentences = CollectionHelper.newArrayList();
        for (Annotation tempSentence : maskedSentences) {
            int start = tempSentence.getStartPosition();
            String value = text.substring(start, tempSentence.getStartPosition() + tempSentence.getValue().length());
            sentences.add(new ImmutableAnnotation(tempSentence.getStartPosition(), value));
        }
        return sentences;
    }

    private static Annotation createAnnotation(String text, int start, int end) {
        String value = text.substring(start, end);
        String leftTrimmedValue = StringHelper.ltrim(value);
        int leftWhitespaceOffset = value.length() - leftTrimmedValue.length();
        String trimmedValue = StringHelper.rtrim(leftTrimmedValue);
        if (trimmedValue.isEmpty()) {
            return null;
        }
        int leftIndex = start + leftWhitespaceOffset;
        return new ImmutableAnnotation(leftIndex, trimmedValue, StringUtils.EMPTY);
    }

}
