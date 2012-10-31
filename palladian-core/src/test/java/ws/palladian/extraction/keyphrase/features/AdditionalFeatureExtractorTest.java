package ws.palladian.extraction.keyphrase.features;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.feature.DuplicateTokenConsolidator;
import ws.palladian.extraction.feature.StemmerAnnotator;
import ws.palladian.extraction.feature.StemmerAnnotator.Mode;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.helper.constants.Language;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.PipelineDocument;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.Annotation;

public class AdditionalFeatureExtractorTest {
    
    @Test
    public void testExtractAdditionalFeatures() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.add(new RegExTokenizer());
        pipeline.add(new StemmerAnnotator(Language.ENGLISH, Mode.MODIFY));
        pipeline.add(new DuplicateTokenConsolidator());
        pipeline.add(new AdditionalFeatureExtractor());
        TextDocument document = pipeline.process(new TextDocument("the quick brown Fox jumps over the lazy Dog. the quick brown Fox jumps over the lazy dog."));
        List<Annotation<String>> tokenAnnotations = RegExTokenizer.getTokenAnnotations(document);
        assertEquals(9, tokenAnnotations.size());
    }

    @Test
    public void testGetPunctuationPercentage() {
        assertEquals(0.5, AdditionalFeatureExtractor.getPunctuationPercentage("a.a.a."), 0);
        assertEquals(0, AdditionalFeatureExtractor.getPunctuationPercentage("aaa"), 0);
        assertEquals(1, AdditionalFeatureExtractor.getPunctuationPercentage("..."), 0);
    }

    @Test
    public void testGetDigitPercentage() {
        assertEquals(0.25, AdditionalFeatureExtractor.getDigitPercentage("abc1"), 0);
    }

    @Test
    public void testGetUniqueCharacterPercentage() {
        assertEquals(0, AdditionalFeatureExtractor.getUniqueCharacterPercentage("a"), 0);
        assertEquals(0, AdditionalFeatureExtractor.getUniqueCharacterPercentage("aaa"), 0);
        assertEquals(0.4, AdditionalFeatureExtractor.getUniqueCharacterPercentage("aaaab"), 0);
        assertEquals(0.5, AdditionalFeatureExtractor.getUniqueCharacterPercentage("aabb"), 0);
        assertEquals(1, AdditionalFeatureExtractor.getUniqueCharacterPercentage("abcd"), 0);
    }

}
