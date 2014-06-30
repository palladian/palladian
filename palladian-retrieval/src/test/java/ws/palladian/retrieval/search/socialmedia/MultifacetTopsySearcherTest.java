package ws.palladian.retrieval.search.socialmedia;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Set;

import org.junit.Test;

public class MultifacetTopsySearcherTest {

    @Test
    public void testExtractIdentifier() {
        String identifier = MultifacetTopsySearcher
                .extractIdentifier("http://twitter.com/Yahoo/status/407136819797450752");
        assertEquals("407136819797450752", identifier);
    }

    @Test
    public void testExtractTags() {
        String content = "BREAKING: @NTSB says union for #metronorth #engineer \"breaches the party agreement\" by describing what train op told investigators #nbc4ny";
        Set<String> tags = MultifacetTopsySearcher.extractTags(content);
        assertEquals(3, tags.size());
        assertTrue(tags.containsAll(Arrays.asList("metronorth", "engineer", "nbc4ny")));
    }

}
