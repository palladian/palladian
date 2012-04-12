package ws.palladian.extraction.keyphrase.features;

import java.util.List;

import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.PipelineProcessor;
import ws.palladian.extraction.feature.Annotation;
import ws.palladian.extraction.feature.AnnotationFeature;
import ws.palladian.extraction.feature.AnnotationGroup;
import ws.palladian.extraction.feature.CountCalculator;
import ws.palladian.extraction.feature.FrequencyCalculator;
import ws.palladian.extraction.token.TokenizerInterface;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;

/**
 * <p>
 * Calculates the "Generalized Dice Coefficient" for {@link AnnotationGroup}s consisting of multiple Tokens based on
 * their occurence counts. Groups of tokens with high co-occurence frequency are weighted highly. See
 * "Automatic Glossary Extraction: Beyond Terminology Identification"; Youngja Park, Roy J Byrd, Branimir K Boguraev,
 * 2002; and "HUMB: Automatic Key Term Extraction from Scientific Articles in GROBID"; Patrice Lopez, Laurent Romary,
 * 2010.
 * </p>
 * 
 * @author Philipp Katz
 */
public class PhrasenessAnnotator implements PipelineProcessor {

    private static final long serialVersionUID = 1L;
    public static final String PROVIDED_FEATURE = "phraseness";

    private static final double SINGLE_WORD_TERM_FACTOR = 0.1;

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(TokenizerInterface.PROVIDED_FEATURE);
        List<Annotation> annotations = annotationFeature.getValue();

        for (Annotation annotation : annotations) {
            NumericFeature phrasenessFeature;
            if (annotation instanceof AnnotationGroup) {
                AnnotationGroup group = (AnnotationGroup) annotation;

                int numberOfTerms = group.getAnnotations().size();
                double phraseCount = getCount(group);
                double wordCount = getCount(group.getAnnotations());
                double diceCoefficient = (numberOfTerms * Math.log10(phraseCount) * phraseCount) / wordCount;

                phrasenessFeature = new NumericFeature(PROVIDED_FEATURE, diceCoefficient);
            } else {

                double phraseCount = getCount(annotation);
                double diceCoefficient = Math.log10(phraseCount) * phraseCount;

                phrasenessFeature = new NumericFeature(PROVIDED_FEATURE, SINGLE_WORD_TERM_FACTOR * diceCoefficient);
            }
            annotation.getFeatureVector().add(phrasenessFeature);
        }
    }

    private double getCount(List<Annotation> annotations) {
        double frequencySum = 0;
        for (Annotation annotation : annotations) {
            frequencySum += getCount(annotation);
        }
        return frequencySum;
    }

    private double getCount(Annotation annotation) {
        if (annotation.getFeatureVector().get(FrequencyCalculator.PROVIDED_FEATURE) == null) {
            // System.err.println("something is wrong for " + annotation);
            return 1;
        }
        double frequency = (Double) annotation.getFeatureVector().get(CountCalculator.PROVIDED_FEATURE).getValue();
        return frequency;
    }

}
