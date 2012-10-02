package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.features.Annotation;
import ws.palladian.processing.features.TextAnnotationFeature;

public class AnnotatorTest {

    private final PipelineDocument<String> document = new PipelineDocument<String>("Let's try to stem some tokens in English language.");

    @Test(expected = DocumentUnprocessableException.class)
    public void testMissingTokenAnnotations() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new StemmerAnnotator(Language.ENGLISH));
        pipeline.process(document);
    }

    @Test
    public void testStemmerAnnotator() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new StemmerAnnotator(Language.ENGLISH));
        pipeline.process(document);

        TextAnnotationFeature annotationFeature = document.getFeatureVector().get(
                BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation<String>> annotations = annotationFeature.getValue();

        assertEquals(12, annotations.size());
        assertEquals("tri", annotations.get(3).getFeature(StemmerAnnotator.STEM).getValue());
        assertEquals("token", annotations.get(7).getFeature(StemmerAnnotator.STEM).getValue());
        assertEquals("languag", annotations.get(10).getFeature(StemmerAnnotator.STEM).getValue());
        
    }

    @Test
    public void testStopTokenRemover() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new StopTokenRemover(Language.ENGLISH));
        pipeline.process(document);

        TextAnnotationFeature annotationFeature = document.getFeatureVector().get(
                BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation<String>> annotations = annotationFeature.getValue();

        assertEquals(7, annotations.size());
    }

    @Test
    public void testTokenLengthRemover() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new LengthTokenRemover(2));
        pipeline.process(document);

        TextAnnotationFeature annotationFeature = document.getFeatureVector().get(
                BaseTokenizer.PROVIDED_FEATURE_DESCRIPTOR);
        List<Annotation<String>> annotations = annotationFeature.getValue();

        assertEquals(9, annotations.size());
    }

}
