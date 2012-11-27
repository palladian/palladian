package ws.palladian.extraction.token;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Test;

import ws.palladian.processing.DocumentUnprocessableException;
import ws.palladian.processing.TextDocument;

public class OpenNlpTokenizerTest {
    
    @Test
    public void testOpenNlpTokenizer() throws DocumentUnprocessableException {
        OpenNlpTokenizer tokenizer = new OpenNlpTokenizer();
        TextDocument document = new TextDocument("The quick brown fox jumps over the lazy dog.");
        tokenizer.processDocument(document);
        
        List<String> tokens = OpenNlpTokenizer.getTokens(document);
        assertEquals(10, tokens.size());
    }

}
