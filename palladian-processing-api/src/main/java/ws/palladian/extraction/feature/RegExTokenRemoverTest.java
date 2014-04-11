package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.ListFeature;
import ws.palladian.processing.features.PositionAnnotation;

/**
 * @author Philipp Katz
 */
public class RegExTokenRemoverTest {

    private TextDocument document;
    private ProcessingPipeline pipeline;

    @Before
    public void setUp() {
        document = new TextDocument("test 273 t_est ; &");
        pipeline = new ProcessingPipeline();
        pipeline.connectToPreviousProcessor(new RegExTokenizer());
    }

    @Test
    public void testRegExTokenRemover() throws DocumentUnprocessableException {
        pipeline.connectToPreviousProcessor(new RegExTokenRemover("[A-Za-z0-9-]+"));
        pipeline.process(document);
        List<PositionAnnotation> annotations = document.get(ListFeature.class,
                BaseTokenizer.PROVIDED_FEATURE);
        assertEquals(2, annotations.size());
        assertEquals("test", annotations.get(0).getValue());
        assertEquals("273", annotations.get(1).getValue());

    }

    @Test
    public void testRegExTokenRemoverInverse() throws DocumentUnprocessableException {
        pipeline.connectToPreviousProcessor(new RegExTokenRemover("\\d+", true));
        document = (TextDocument)pipeline.process(document);
        List<PositionAnnotation> annotations = document.get(ListFeature.class,
                BaseTokenizer.PROVIDED_FEATURE);
        assertEquals(4, annotations.size());
    }

}
