package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.TextAnnotationFeature;

/**
 * @author Philipp Katz
 */
public class RegExTokenRemoverTest {

    private static final PipelineDocument<String> DOCUMENT = new PipelineDocument<String>("test 273 t_est ; •");
    private ProcessingPipeline pipeline;

    @Before
    public void setUp() {
        pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
    }

    @Test
    public void testRegExTokenRemover() throws DocumentUnprocessableException {
        pipeline.add(new RegExTokenRemover("[A-Za-z0-9-]+"));
        PipelineDocument<String> document = pipeline.process(DOCUMENT);
        TextAnnotationFeature annotationFeature = document.getFeatureVector().get(
                RegExTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation<String>> annotations = annotationFeature.getValue();
        assertEquals(2, annotations.size());
        assertEquals("test", annotations.get(0).getValue());
        assertEquals("273", annotations.get(1).getValue());

    }

    @Test
    public void testRegExTokenRemoverInverse() throws DocumentUnprocessableException {
        pipeline.add(new RegExTokenRemover("\\d+", true));
        PipelineDocument<String> document = pipeline.process(DOCUMENT);
        TextAnnotationFeature annotationFeature = document.getFeatureVector().get(
                RegExTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation<String>> annotations = annotationFeature.getValue();
        assertEquals(4, annotations.size());
    }

}
