package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.DocumentUnprocessableException;
import ws.palladian.extraction.PipelineDocument;
import ws.palladian.extraction.ProcessingPipeline;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.model.features.Annotation;
import ws.palladian.model.features.AnnotationFeature;

/**
 * @author Philipp Katz
 */
public class RegExTokenRemoverTest {
    
    @Test
    public void testRegExTokenRemover() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new RegExTokenRemover("[^A-Za-z0-9-]+"));
        PipelineDocument document = pipeline.process(new PipelineDocument("test 273 . ; •"));
        AnnotationFeature annotationFeature = document.getFeatureVector().get(RegExTokenizer.PROVIDED_FEATURE_DESCRIPTOR);;
        List<Annotation> annotations = annotationFeature.getValue();
        assertEquals(2, annotations.size());
        assertEquals("test", annotations.get(0).getValue());
        assertEquals("273", annotations.get(1).getValue());
        
    }

}
