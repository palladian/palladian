/**
 * Created on: 16.06.2012 11:32:26
 */
package ws.palladian.extraction.feature;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.Port;
import ws.palladian.processing.features.AbstractFeatureProvider;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.FeatureDescriptor;
import ws.palladian.processing.features.NumericFeature;

/**
 * <p>
 * Calculates the Jaccard similarity (see <a href="http://en.wikipedia.org/wiki/Jaccard_index">Jaccard index</a>) as a
 * measure of overlap between two sets of the same {@link Annotation} from two {@link PipelineDocument}s. The processor
 * provides two input ports identified by {@link #INPUT_PORT_ONE_IDENTIFIER} and
 * {@link TokenOverlapCalculator#INPUT_PORT_TWO_IDENTIFIER}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class TokenOverlapCalculator extends AbstractFeatureProvider<Object, NumericFeature> {

    /**
     * <p>
     * Used for serializing objects of this class. Should only change if the attribute set changes.
     * </p>
     */
    private static final long serialVersionUID = 3094845412635456119L;

    public static final String INPUT_PORT_ONE_IDENTIFIER = "input1";
    public static final String INPUT_PORT_TWO_IDENTIFIER = "input2";

    private final FeatureDescriptor<? extends AnnotationFeature<?>> input1FeatureDescriptor;
    private final FeatureDescriptor<? extends AnnotationFeature<?>> input2FeatureDescriptor;

    /**
     * <p>
     * Creates a new {@code TokenOverlapCalculator} which takes the {@code Feature}s from the {@code PipelineDocument}
     * at the {@code Port} identified by {@link #INPUT_PORT_ONE_IDENTIFIER} and calculates the Jaccard similarity with
     * the {@code Feature}s from the {@code PipelineDocument} at the {@code Port} identified by
     * {@link #INPUT_PORT_TWO_IDENTIFIER}. The {@code Feature}s from the first {@code PipelineDocument} are identified
     * by {@code input1FeatureDescriptor} while the {@code Feature}s from the second {@code PipelineDocument} are
     * identified by {@code input2FeatureDescriptor}.
     * </p>
     * 
     * @param featureDescriptor The descriptor for the result {@code Feature}.
     * @param input1FeatureDescriptor The descriptor for the first input {@code Feature}.
     * @param input2FeatureDescriptor The descriptor for the second input {@code Feature}.
     */
    public TokenOverlapCalculator(final FeatureDescriptor<NumericFeature> featureDescriptor,
            final FeatureDescriptor<? extends AnnotationFeature<?>> input1FeatureDescriptor,
            final FeatureDescriptor<? extends AnnotationFeature<?>> input2FeatureDescriptor) {
        // Ports parameterized with Objects since it does not matter which type they have, because the Calculator only
        // uses the feature vector.
        // FIXME omfg, we have to think of a cleaner solution here, look at all those warning! -- 2012-08-24, Philipp
        super((List)Arrays.asList(new Port[] {new Port<Object>(INPUT_PORT_ONE_IDENTIFIER),
                new Port<Object>(INPUT_PORT_TWO_IDENTIFIER)}), (List)Arrays.asList(new Port[] {new Port<Object>(
                PipelineProcessor.DEFAULT_OUTPUT_PORT_IDENTIFIER)}), featureDescriptor);

        this.input1FeatureDescriptor = input1FeatureDescriptor;
        this.input2FeatureDescriptor = input2FeatureDescriptor;
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<?> document1 = getInputPort(INPUT_PORT_ONE_IDENTIFIER).getPipelineDocument();
        PipelineDocument<?> document2 = getInputPort(INPUT_PORT_TWO_IDENTIFIER).getPipelineDocument();

        AnnotationFeature feature1 = document1.getFeature(input1FeatureDescriptor);
        Validate.notNull(feature1, "No feature found for feature descriptor " + input1FeatureDescriptor);
        final List<Annotation<?>> input1Annotations = (List<Annotation<?>>)feature1.getValue();
        AnnotationFeature feature2 = document2.getFeature(input2FeatureDescriptor);
        Validate.notNull(feature2, "No feature found for feature descriptor " + input2FeatureDescriptor);
        final List<Annotation<?>> input2Annotations = (List<Annotation<?>>)feature2.getValue();

        Set<String> setOfInput1 = new HashSet<String>();
        Set<String> setOfInput2 = new HashSet<String>();
        for (Annotation annotation : input1Annotations) {
            setOfInput1.add(annotation.getValue());
        }
        for (Annotation annotation : input2Annotations) {
            setOfInput2.add(annotation.getValue());
        }
        final Set<String> overlap = new HashSet<String>();
        overlap.addAll(setOfInput1);
        overlap.retainAll(setOfInput2);
        final Set<String> union = new HashSet<String>();
        union.addAll(setOfInput1);
        union.addAll(setOfInput2);

        Double jaccardSimilarity = Integer.valueOf(overlap.size()).doubleValue()
                / Integer.valueOf(union.size()).doubleValue();
        // TODO Remove debug code
        if (jaccardSimilarity > 1.0) {
            System.out.println("+++++++++++++++++");
        }

        document1.addFeature(new NumericFeature(getDescriptor(), jaccardSimilarity));
        setOutput(DEFAULT_OUTPUT_PORT_IDENTIFIER, document1);
    }
}
