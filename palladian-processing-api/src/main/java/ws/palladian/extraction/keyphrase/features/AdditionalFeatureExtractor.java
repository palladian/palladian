package ws.palladian.extraction.keyphrase.features;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import ws.palladian.extraction.feature.DuplicateTokenConsolidator;
import ws.palladian.extraction.feature.DuplicateTokenRemover;
import ws.palladian.extraction.feature.Stemmer;
import ws.palladian.extraction.feature.TextDocumentPipelineProcessor;
import ws.palladian.extraction.token.AbstractTokenizer;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.CountMap;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Annotator for various keyphrase extraction specific features. Requires documents to be processed by a
 * {@link AbstractTokenizer}, a {@link Stemmer} and a {@link DuplicateTokenConsolidator} or
 * {@link DuplicateTokenRemover} first.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class AdditionalFeatureExtractor extends TextDocumentPipelineProcessor {

    /** Denotes the percentage a term instance starts with an upper case letter. */
    public static final String STARTS_UPPERCASE_PERCENTAGE = "startsUppercase";
    /** Denotes the percentage a term instance occurs completely upper cased. */
    public static final String COMPLETE_UPPERCASE = "completelyUppercase";
    /** Denotes the percentage of digits in a term. */
    public static final String NUMBER_PERCENTAGE = "containsNumbers";
    /** Denotes whether the term is a number. */
    public static final String IS_NUMBER = "isNumber";
    /** Denotes the percentage of punctuation characters in the term. */
    public static final String PUNCTUATION_PERCENTAGE = "containsPunctuation";
    /** Denotes the percentage of unique characters in the term. */
    public static final String UNIQUE_CHARACTER_PERCENTAGE = "uniqueCharacterPercentage";
    /** Denotes the case signature of the must common variant of this term. */
    public static final String CASE_SIGNATURE = "caseSignature";

    // further features to consider:
    // containsSpecialCharacters
    // previousStopword, nextStopword, ...
    // NER
    // isInBrackets
    // isInQuotes
    // positionInSentence (begin|middle|end)
    // gerund (-ing?)

    public AdditionalFeatureExtractor() {
    }

    @Override
    public void processDocument(TextDocument document) throws DocumentUnprocessableException {
        List<PositionAnnotation> annotations = document.get(ListFeature.class, AbstractTokenizer.PROVIDED_FEATURE);
        for (int i = 0; i < annotations.size(); i++) {
            PositionAnnotation annotation = annotations.get(i);
            String unstemValue = annotation.getFeatureVector().get(NominalFeature.class, Stemmer.UNSTEM).getValue();
            if (unstemValue == null) {
                throw new DocumentUnprocessableException("The necessary feature \"" + Stemmer.UNSTEM
                        + "\" is missing for Annotation \"" + annotation.getValue() + "\"");
            }

            double startsUppercase = getStartsUppercase(annotation);
            double completeUppercase = getCompleteUppercase(annotation);
            double numberCount = getDigitPercentage(unstemValue);
            String caseSignature = getCaseSignature(annotation);
            String isNumber = String.valueOf(getIsNumber(unstemValue));
            double punctuationPercentage = getPunctuationPercentage(unstemValue);
            double uniqueCharacterPercentage = getUniqueCharacterPercentage(unstemValue);

            FeatureVector featureVector = annotation.getFeatureVector();
            featureVector.add(new NumericFeature(STARTS_UPPERCASE_PERCENTAGE, startsUppercase));
            featureVector.add(new NumericFeature(COMPLETE_UPPERCASE, completeUppercase));
            featureVector.add(new NumericFeature(NUMBER_PERCENTAGE, numberCount));
            featureVector.add(new NominalFeature(IS_NUMBER, isNumber));
            featureVector.add(new NumericFeature(UNIQUE_CHARACTER_PERCENTAGE, punctuationPercentage));
            featureVector.add(new NumericFeature(UNIQUE_CHARACTER_PERCENTAGE, uniqueCharacterPercentage));
            featureVector.add(new NominalFeature(CASE_SIGNATURE, caseSignature));
        }
    }

    static double getUniqueCharacterPercentage(String value) {
        CountMap<Character> characters = CountMap.create();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            characters.add(c);
        }
        if (characters.uniqueSize() == 1) {
            return 0;
        }
        return (double)(characters.uniqueSize()) / value.length();
    }

    static double getPunctuationPercentage(String value) {
        double punctuationCount = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (StringHelper.isPunctuation(c)) {
                punctuationCount++;
            }
        }
        return punctuationCount / value.length();
    }

    private boolean getIsNumber(String value) {
        return StringHelper.isNumber(value);
    }

    static double getDigitPercentage(String value) {
        double digitCount = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (Character.isDigit(c)) {
                digitCount++;
            }
        }
        return digitCount / value.length();
    }

    private double getCompleteUppercase(PositionAnnotation annotation) {
        List<PositionAnnotation> allAnnotations = CollectionHelper.newArrayList();
        allAnnotations.add(annotation);
        allAnnotations.addAll(DuplicateTokenConsolidator.getDuplicateAnnotations(annotation));

        double completeUppercaseCount = 0;
        for (PositionAnnotation current : allAnnotations) {
            if (StringUtils.isAllUpperCase(current.getFeatureVector().get(NominalFeature.class, Stemmer.UNSTEM).getValue())) {
                completeUppercaseCount++;
            }
        }
        // FIXME return completeUppercaseCount / allAnnotations.size();
        return completeUppercaseCount / allAnnotations.size() > 0.5 ? 1 : 0;
    }

    private String getCaseSignature(PositionAnnotation annotation) {
        List<PositionAnnotation> allAnnotations = CollectionHelper.newArrayList();
        allAnnotations.add(annotation);
        allAnnotations.addAll(DuplicateTokenConsolidator.getDuplicateAnnotations(annotation));

        CountMap<String> signatures = CountMap.create();
        for (PositionAnnotation current : allAnnotations) {
            String caseSignature = StringHelper.getCaseSignature(current.getFeatureVector()
                    .get(NominalFeature.class, Stemmer.UNSTEM).getValue());
            signatures.add(caseSignature);
        }
        return signatures.getHighest();
    }

    private double getStartsUppercase(PositionAnnotation annotation) {
        List<PositionAnnotation> allAnnotations = CollectionHelper.newArrayList();
        allAnnotations.add(annotation);
        allAnnotations.addAll(DuplicateTokenConsolidator.getDuplicateAnnotations(annotation));

        double uppercaseCount = 0;
        for (PositionAnnotation current : allAnnotations) {
            if (StringHelper.startsUppercase(current.getFeatureVector().get(NominalFeature.class, Stemmer.UNSTEM).getValue())) {
                uppercaseCount++;
            }
        }
        // FIXME return uppercaseCount / allAnnotations.size();
        return uppercaseCount / allAnnotations.size() > 0.5 ? 1 : 0;
    }

}
