package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.token.BaseTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.NominalFeature;
import ws.palladian.processing.features.PositionAnnotation;

public class AnnotatorTest {

    private final TextDocument document = new TextDocument("Let's try to stem some tokens in English language.");

    // XXX there is no exception triggered in this case any longer ...:
    
//    @Test(expected = DocumentUnprocessableException.class)
//    public void testMissingTokenAnnotations() throws DocumentUnprocessableException {
//        ProcessingPipeline pipeline = new ProcessingPipeline();
//        pipeline.add(new StemmerAnnotator(Language.ENGLISH));
//        pipeline.process(document);
//    }

    @Test
    public void testStemmerAnnotator() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new StemmerAnnotator(Language.ENGLISH));
        pipeline.process(document);

        List<PositionAnnotation> annotations = document.getFeatureVector().getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);

        assertEquals(12, annotations.size());
        assertEquals("tri", annotations.get(3).getFeatureVector().getFeature(NominalFeature.class, StemmerAnnotator.STEM).getValue());
        assertEquals("token", annotations.get(7).getFeatureVector().getFeature(NominalFeature.class, StemmerAnnotator.STEM).getValue());
        assertEquals("languag", annotations.get(10).getFeatureVector().getFeature(NominalFeature.class, StemmerAnnotator.STEM).getValue());
        
    }

    @Test
    public void testStopTokenRemover() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new StopTokenRemover(Language.ENGLISH));
        pipeline.process(document);

        List<PositionAnnotation> annotations = document.getFeatureVector().getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);

        assertEquals(7, annotations.size());
    }

    @Test
    public void testTokenLengthRemover() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new LengthTokenRemover(2));
        pipeline.process(document);

        List<PositionAnnotation> annotations = document.getFeatureVector().getAll(PositionAnnotation.class, BaseTokenizer.PROVIDED_FEATURE);

        assertEquals(9, annotations.size());
    }

}
