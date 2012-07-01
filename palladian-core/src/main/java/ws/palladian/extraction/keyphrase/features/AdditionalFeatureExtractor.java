package ws.palladian.extraction.keyphrase.features;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.collections15.Bag;
import org.apache.commons.collections15.bag.HashBag;
import org.apache.commons.lang3.StringUtils;

import ws.palladian.extraction.feature.StringDocumentPipelineProcessor;
import ws.palladian.extraction.feature.DuplicateTokenConsolidator;
import ws.palladian.extraction.feature.DuplicateTokenRemover;
import ws.palladian.extraction.feature.StemmerAnnotator;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.helper.collection.BagHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.FeatureDescriptorBuilder;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Annotator for various keyphrase extraction specific features. Requires documents to be processed by a
 * {@link BaseTokenizer}, a {@link StemmerAnnotator} and a {@link DuplicateTokenConsolidator} or
 * {@link DuplicateTokenRemover} first.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class AdditionalFeatureExtractor extends StringDocumentPipelineProcessor {

    private static final long serialVersionUID = 1L;

    /** Denotes the percentage a term instance starts with an upper case letter. */
    public static final FeatureDescriptor<NumericFeature> STARTS_UPPERCASE_PERCENTAGE = FeatureDescriptorBuilder.build(
            "startsUppercase", NumericFeature.class);
    /** Denotes the percentage a term instance occurs completely upper cased. */
    public static final FeatureDescriptor<NumericFeature> COMPLETE_UPPERCASE = FeatureDescriptorBuilder.build(
            "completelyUppercase", NumericFeature.class);
    /** Denotes the percentage of digits in a term. */
    public static final FeatureDescriptor<NumericFeature> NUMBER_PERCENTAGE = FeatureDescriptorBuilder.build(
            "containsNumbers", NumericFeature.class);
    /** Denotes whether the term is a number. */
    public static final FeatureDescriptor<NominalFeature> IS_NUMBER = FeatureDescriptorBuilder.build("isNumber",
            NominalFeature.class);
    /** Denotes the percentage of punctuation characters in the term. */
    public static final FeatureDescriptor<NumericFeature> PUNCTUATION_PERCENTAGE = FeatureDescriptorBuilder.build(
            "containsPunctuation", NumericFeature.class);
    /** Denotes the percentage of unique characters in the term. */
    public static final FeatureDescriptor<NumericFeature> UNIQUE_CHARACTER_PERCENTAGE = FeatureDescriptorBuilder.build(
            "uniqueCharacterPercentage", NumericFeature.class);
    /** Denotes the case signature of the must common variant of this term. */
    public static final FeatureDescriptor<NominalFeature> CASE_SIGNATURE = FeatureDescriptorBuilder.build(
            "caseSignature", NominalFeature.class);

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
    public void processDocument(PipelineDocument<String> document) throws DocumentUnprocessableException {
        AnnotationFeature annotationFeature = document.getFeatureVector()
                .get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();
        for (int i = 0; i < annotations.size(); i++) {
            Annotation annotation = annotations.get(i);
            String unstemValue = annotation.getFeatureVector().get(StemmerAnnotator.UNSTEM).getValue();
            if (unstemValue == null) {
                throw new DocumentUnprocessableException("The necessary feature \"" + StemmerAnnotator.UNSTEM
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
        Bag<Character> characters = new HashBag<Character>();
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            characters.add(c);
        }
        if (characters.uniqueSet().size() == 1) {
            return 0;
        }
        return (double)(characters.uniqueSet().size()) / value.length();
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

    private double getCompleteUppercase(Annotation annotation) {
        List<Annotation> allAnnotations = new ArrayList<Annotation>();
        allAnnotations.add(annotation);
        allAnnotations.addAll(DuplicateTokenConsolidator.getDuplicateAnnotations(annotation));

        double completeUppercaseCount = 0;
        for (Annotation current : allAnnotations) {
            if (StringUtils.isAllUpperCase(current.getFeatureVector().get(StemmerAnnotator.UNSTEM).getValue())) {
                completeUppercaseCount++;
            }
        }
        // FIXME return completeUppercaseCount / allAnnotations.size();
        return completeUppercaseCount / allAnnotations.size() > 0.5 ? 1 : 0;
    }

    private String getCaseSignature(Annotation annotation) {
        List<Annotation> allAnnotations = new ArrayList<Annotation>();
        allAnnotations.add(annotation);
        allAnnotations.addAll(DuplicateTokenConsolidator.getDuplicateAnnotations(annotation));

        Bag<String> signatures = new HashBag<String>();
        for (Annotation current : allAnnotations) {
            String caseSignature = StringHelper.getCaseSignature(current.getFeatureVector()
                    .get(StemmerAnnotator.UNSTEM).getValue());
            signatures.add(caseSignature);
        }
        return BagHelper.getHighest(signatures);
    }

    private double getStartsUppercase(Annotation annotation) {
        List<Annotation> allAnnotations = new ArrayList<Annotation>();
        allAnnotations.add(annotation);
        allAnnotations.addAll(DuplicateTokenConsolidator.getDuplicateAnnotations(annotation));

        double uppercaseCount = 0;
        for (Annotation current : allAnnotations) {
            if (StringHelper.startsUppercase(current.getFeatureVector().get(StemmerAnnotator.UNSTEM).getValue())) {
                uppercaseCount++;
            }
        }
        // FIXME return uppercaseCount / allAnnotations.size();
        return uppercaseCount / allAnnotations.size() > 0.5 ? 1 : 0;
    }

}
