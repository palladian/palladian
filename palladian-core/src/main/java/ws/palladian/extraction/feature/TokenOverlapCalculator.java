/**
 * Created on: 16.06.2012 11:32:26
 */
package ws.palladian.extraction.feature;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.InputPort;
import ws.palladian.processing.OutputPort;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.PipelineProcessor;
import ws.palladian.processing.features.AbstractFeatureProvider;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * <p>
 * Calculates the Jaccard similarity (see <a href="http://en.wikipedia.org/wiki/Jaccard_index">Jaccard index</a>) as a
 * measure of overlap between two sets of the same {@link PositionAnnotation} from two {@link PipelineDocument}s. The
 * processor provides two input ports identified by {@link #INPUT_PORT_ONE_IDENTIFIER} and
 * {@link TokenOverlapCalculator#INPUT_PORT_TWO_IDENTIFIER}.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.1.7
 */
public final class TokenOverlapCalculator extends AbstractFeatureProvider {

    public static final String INPUT_PORT_ONE_IDENTIFIER = "input1";
    public static final String INPUT_PORT_TWO_IDENTIFIER = "input2";

    private final String input1FeatureName;
    private final String input2FeatureName;

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
    public TokenOverlapCalculator(String featureDescriptor, String input1FeatureIdentifier,
            String input2FeatureIdentifier) {
        super(new InputPort[] {new InputPort(INPUT_PORT_ONE_IDENTIFIER), new InputPort(INPUT_PORT_TWO_IDENTIFIER)},
                new OutputPort[] {new OutputPort(PipelineProcessor.DEFAULT_OUTPUT_PORT_IDENTIFIER)}, featureDescriptor);

        this.input1FeatureName = input1FeatureIdentifier;
        this.input2FeatureName = input2FeatureIdentifier;
    }

    @Override
    protected void processDocument() throws DocumentUnprocessableException {
        PipelineDocument<?> document1 = getInputPort(INPUT_PORT_ONE_IDENTIFIER).poll();
        PipelineDocument<?> document2 = getInputPort(INPUT_PORT_TWO_IDENTIFIER).poll();

        final List<PositionAnnotation> input1Annotations = document1.get(
                ListFeature.class, input1FeatureName);
        final List<PositionAnnotation> input2Annotations = document2.get(
                ListFeature.class, input2FeatureName);

        Set<String> setOfInput1 = new HashSet<String>();
        Set<String> setOfInput2 = new HashSet<String>();
        for (PositionAnnotation annotation : input1Annotations) {
            setOfInput1.add(annotation.getValue());
        }
        for (PositionAnnotation annotation : input2Annotations) {
            setOfInput2.add(annotation.getValue());
        }
        final Set<String> intersection = new HashSet<String>();
        intersection.addAll(setOfInput1);
        intersection.retainAll(setOfInput2);
        final Set<String> union = new HashSet<String>();
        union.addAll(setOfInput1);
        union.addAll(setOfInput2);

        Double jaccardSimilarity = Integer.valueOf(intersection.size()).doubleValue()
                / Integer.valueOf(union.size()).doubleValue();
        // TODO Remove debug code
        // if (jaccardSimilarity > 1.0) {
        // System.out.println("+++++++++++++++++");
        // }

        document1.add(new NumericFeature(getCreatedFeatureName(), jaccardSimilarity));
        getOutputPort(DEFAULT_OUTPUT_PORT_IDENTIFIER).put(document1);
    }
}
