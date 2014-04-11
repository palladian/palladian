package ws.palladian.extraction.pos;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.processing.features.Annotation;

public class OpenNlpPosTaggerTest {

    private static final String TEXT = "The quick brown fox jumps over the lazy dog.";
    private File modelFile;

    @Before
    public void setUp() throws FileNotFoundException {
        modelFile = ResourceHelper.getResourceFile("/model/en-pos-maxent.bin");
    }

    @Test
    public void testOpenNlpPosTagger() {
        OpenNlpPosTagger posTagger = new OpenNlpPosTagger(modelFile);
        List<Annotation> annotations = posTagger.getAnnotations(TEXT);
        assertEquals(10, annotations.size());
        assertEquals("DT", annotations.get(0).getTag());
        assertEquals("JJ", annotations.get(1).getTag());
        assertEquals("JJ", annotations.get(2).getTag());
        assertEquals("NN", annotations.get(3).getTag());
        assertEquals("NNS", annotations.get(4).getTag());
        assertEquals("IN", annotations.get(5).getTag());
        assertEquals("DT", annotations.get(6).getTag());
        assertEquals("JJ", annotations.get(7).getTag());
        assertEquals("NN", annotations.get(8).getTag());
        assertEquals(".", annotations.get(9).getTag());
    }

}
