package ws.palladian.extraction.keyphrase.features;

import java.util.List;

import ws.palladian.extraction.AbstractPipelineProcessor;
import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.feature.TokenMetricsCalculator;
import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;
import ws.palladian.model.features.AnnotationGroup;
import ws.palladian.model.features.FeatureDescriptor;
import ws.palladian.model.features.FeatureDescriptorBuilder;
import ws.palladian.model.features.FeatureVector;
import ws.palladian.model.features.NumericFeature;

/**
 * <p>
 * Calculates a <i>Generalized Dice Coefficient</i> for {@link Annotation}s consisting of multiple Tokens based on their
 * occurrence counts. Groups of tokens with high co-occurrence frequencies are weighted highly. See
 * "Automatic Glossary Extraction: Beyond Terminology Identification"; Youngja Park, Roy J Byrd, Branimir K Boguraev,
 * 2002; and "HUMB: Automatic Key Term Extraction from Scientific Articles in GROBID"; Patrice Lopez, Laurent Romary,
 * 2010. The Generalized Dice Coefficient is defined as:
 * 
 * <pre>
 *  |T| * log10(f(T)) * f(T)
 * ——————————————————————————
 *   sum_(w_i in T) f(w_i)
 * </pre>
 * 
 * Where <code>|T|</code> is the number of words in Term <code>T</code>, <code>f(T)</code> is the count of Term
 * <code>T</code>, <code>f(w_i)</code> is the frequency of word <code>w_i</code>. As the weight of single word terms
 * only depends on their own frequencies and therefore results in much bigger results than for multi-word terms, these
 * terms are scaled down to a certain fraction, usually 0.1.
 * </p>
 * 
 * @author Philipp Katz
 */
public final class PhrasenessAnnotator extends AbstractPipelineProcessor {

    private static final long serialVersionUID = 1L;

    /** The {@link FeatureDescriptor} describing the Generalized Dice Coefficient annotated by this component. */
    public static final FeatureDescriptor<NumericFeature> GENERALIZED_DICE = FeatureDescriptorBuilder.build(
            "ws.palladian.features.generalizedDice", NumericFeature.class);

    /** Default factor for weighting single word terms, as suggested by Park, Byrd, Boguraev. */
    private static final double DEFAULT_SINGLE_WORD_TERM_FACTOR = 0.1;

    /** The defined factor used for scaling single word terms. */
    private final double singleWordTermFactor;

    /**
     * <p>
     * Create a new {@link PhrasenessAnnotator} with the specified factor for weighting single word terms.
     * </p>
     * 
     * @param singleWordTermFactor The factor with which to weight single word phrases.
     */
    public PhrasenessAnnotator(double singleWordTermFactor) {
        this.singleWordTermFactor = singleWordTermFactor;
    }

    /**
     * <p>
     * Create a new {@link PhrasenessAnnotator} with the default factor for weighting single word terms (0.1).
     * </p>
     */
    public PhrasenessAnnotator() {
        this(DEFAULT_SINGLE_WORD_TERM_FACTOR);
    }

    @Override
    protected void processDocument(PipelineDocument document) throws DocumentUnprocessableException {
        FeatureVector featureVector = document.getFeatureVector();
        AnnotationFeature annotationFeature = featureVector.get(BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation> annotations = annotationFeature.getValue();

        for (Annotation annotation : annotations) {
            NumericFeature generalizedDiceFeature;
            double phraseCount = getCount(annotation);

            if (annotation instanceof AnnotationGroup) {
                AnnotationGroup group = (AnnotationGroup)annotation;
                int numberOfTerms = group.getAnnotations().size();
                double wordCount = getCount(group.getAnnotations());
                double generalizedDice = (numberOfTerms * Math.log10(phraseCount) * phraseCount) / wordCount;
                generalizedDiceFeature = new NumericFeature(GENERALIZED_DICE, generalizedDice);
            } else {
                double generalizedDice = Math.log10(phraseCount);
                generalizedDiceFeature = new NumericFeature(GENERALIZED_DICE, singleWordTermFactor * generalizedDice);
            }
            annotation.getFeatureVector().add(generalizedDiceFeature);
        }
    }

    private double getCount(List<Annotation> annotations) throws DocumentUnprocessableException {
        double count = 0;
        for (Annotation annotation : annotations) {
            count += getCount(annotation);
        }
        return count;
    }

    private double getCount(Annotation annotation) throws DocumentUnprocessableException {
        NumericFeature countFeature = annotation.getFeatureVector().get(TokenMetricsCalculator.COUNT);
        if (countFeature == null) {
            throw new DocumentUnprocessableException("Expected feature \"" + TokenMetricsCalculator.COUNT
                    + "\" is missing.");
        }
        return countFeature.getValue();
    }

    /*
     * (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("PhrasenessAnnotator [singleWordTermFactor=");
        builder.append(singleWordTermFactor);
        builder.append("]");
        return builder.toString();
    }

}
