package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.FeatureVector;
import ws.palladian.processing.features.NumericFeature;
import ws.palladian.processing.features.PositionAnnotation;

public class TokenMetricsCalculatorTest {

    private static final String SAMPLE_TEXT = "Das Reh springt hoch, das Reh springt weit. Warum auch nicht - es hat ja Zeit!";

    @Test
    public void testTokenMetrics() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.addWithDefaultConnection(new RegExTokenizer());
        pipeline.addWithDefaultConnection(new TokenMetricsCalculator());
        TextDocument document = (TextDocument)pipeline.process(new TextDocument(SAMPLE_TEXT));

        List<PositionAnnotation> annotations = document.getFeatureVector().getAll(PositionAnnotation.class,
                RegExTokenizer.PROVIDED_FEATURE);

        PositionAnnotation token = annotations.get(1);
        FeatureVector tokenFeatureVector = token.getFeatureVector();
        assertEquals("Reh", token.getValue());
        assertEquals(1. / 18, (double)tokenFeatureVector.getFeature(NumericFeature.class, TokenMetricsCalculator.FIRST)
                .getValue(), 0);
        assertEquals(6. / 18., (double)tokenFeatureVector.getFeature(NumericFeature.class, TokenMetricsCalculator.LAST)
                .getValue(), 0);
        assertEquals(2, (double)tokenFeatureVector.getFeature(NumericFeature.class, TokenMetricsCalculator.COUNT)
                .getValue(), 0);
        assertEquals(1, (double)tokenFeatureVector.getFeature(NumericFeature.class, TokenMetricsCalculator.FREQUENCY)
                .getValue(), 0);
        assertEquals(5. / 18.,
                (double)tokenFeatureVector.getFeature(NumericFeature.class, TokenMetricsCalculator.SPREAD).getValue(),
                0);
        assertEquals(3, (double)tokenFeatureVector.getFeature(NumericFeature.class, TokenMetricsCalculator.CHAR_LENGTH)
                .getValue(), 0);
        assertEquals(1., (double)tokenFeatureVector
                .getFeature(NumericFeature.class, TokenMetricsCalculator.WORD_LENGTH).getValue(), 0);
    }

}
