package ws.palladian.extraction.token;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.processing.features.Annotation;

public class OpenNlpTokenizerTest {

    private static final String TEXT = "The quick brown fox jumps over the lazy dog.";

    @Test
    public void testOpenNlpTokenizer() {
        OpenNlpTokenizer tokenizer = new OpenNlpTokenizer();
        List<Annotation> tokens = tokenizer.getAnnotations(TEXT);
        assertEquals(10, tokens.size());
    }

}
