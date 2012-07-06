package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.AnnotationFeature;
import ws.palladian.processing.features.FeatureVector;

public class TokenMetricsCalculatorTest {

    private static final String SAMPLE_TEXT = "Das Reh springt hoch, das Reh springt weit. Warum auch nicht - es hat ja Zeit!";

    @Test
    public void testTokenMetrics() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new TokenMetricsCalculator());
        PipelineDocument<String> document = pipeline.process(new PipelineDocument<String>(SAMPLE_TEXT));

        AnnotationFeature<String> annotations = document.getFeatureVector().get(RegExTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation<String>> tokens = annotations.getValue();

        Annotation<String> token = tokens.get(1);
        assertEquals("Reh", token.getValue());
        FeatureVector featureVector = token.getFeatureVector();
        assertEquals(1. / 18, (double)featureVector.get(TokenMetricsCalculator.FIRST).getValue(), 0);
        assertEquals(6. / 18., (double)featureVector.get(TokenMetricsCalculator.LAST).getValue(), 0);
        assertEquals(2, (double)featureVector.get(TokenMetricsCalculator.COUNT).getValue(), 0);
        assertEquals(1, (double)featureVector.get(TokenMetricsCalculator.FREQUENCY).getValue(), 0);
        assertEquals(5. / 18., (double)featureVector.get(TokenMetricsCalculator.SPREAD).getValue(), 0);
        assertEquals(3, (double)featureVector.get(TokenMetricsCalculator.CHAR_LENGTH).getValue(), 0);
        assertEquals(1., (double)featureVector.get(TokenMetricsCalculator.WORD_LENGTH).getValue(), 0);
    }

}
