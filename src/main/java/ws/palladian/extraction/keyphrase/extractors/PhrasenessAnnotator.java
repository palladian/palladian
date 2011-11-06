package ws.palladian.extraction.keyphrase.extractors;

import java.util.List;

import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;
import ws.palladian.preprocessing.PipelineDocument;
import ws.palladian.preprocessing.PipelineProcessor;
import ws.palladian.preprocessing.featureextraction.Annotation;
import ws.palladian.preprocessing.featureextraction.AnnotationFeature;
import ws.palladian.preprocessing.featureextraction.AnnotationGroup;
import ws.palladian.preprocessing.featureextraction.CountCalculator;
import ws.palladian.preprocessing.featureextraction.FrequencyCalculator;
import ws.palladian.preprocessing.featureextraction.Tokenizer;

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
    private static final String PROVIDED_FEATURE = "phraseness";

    private static final double SINGLE_WORD_TERM_FACTOR = 0.1;

    @Override
    public void process(PipelineDocument document) {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = (AnnotationFeature) featureVector.get(Tokenizer.PROVIDED_FEATURE);
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
            System.err.println("something is wrong for " + annotation);
            return 0;
        }
        double frequency = (Double) annotation.getFeatureVector().get(CountCalculator.PROVIDED_FEATURE).getValue();
        return frequency;
    }

}
