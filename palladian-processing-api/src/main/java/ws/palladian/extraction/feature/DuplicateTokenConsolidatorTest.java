package ws.palladian.extraction.feature;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.extraction.token.AbstractTokenizer;
import ws.palladian.extraction.token.RegExTokenizer;
import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.ProcessingPipeline;
import ws.palladian.processing.TextDocument;
import ws.palladian.processing.features.PositionAnnotation;

public class DuplicateTokenConsolidatorTest {

    private static final String SAMPLE_TEXT = "Das Reh springt hoch, das Reh springt weit. Warum auch nicht - es hat ja Zeit!";

    @Test
    public void testDuplicateTokenConsolidator() throws DocumentUnprocessableException {
        ProcessingPipeline pipeline = new ProcessingPipeline();
        pipeline.connectToPreviousProcessor(new RegExTokenizer());
        pipeline.connectToPreviousProcessor(new DuplicateTokenConsolidator());
        TextDocument document = (TextDocument)pipeline.process(new TextDocument(SAMPLE_TEXT));

        List<PositionAnnotation> tokenAnnotations = AbstractTokenizer.getTokenAnnotations(document);
        PositionAnnotation token1 = tokenAnnotations.get(0);
        assertEquals("Das", token1.getValue());
        List<PositionAnnotation> duplicates1 = DuplicateTokenConsolidator.getDuplicateAnnotations(token1);
        assertEquals(1, duplicates1.size());
        assertEquals(22, duplicates1.get(0).getStartPosition());
        assertEquals(25, duplicates1.get(0).getEndPosition());
        assertEquals("das", duplicates1.get(0).getValue());
    }

}
