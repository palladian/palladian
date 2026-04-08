package ws.palladian.retrieval.search.images;

import org.junit.Test;
import ws.palladian.retrieval.search.SearcherException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PixabaySearcherTest {
    @Test
    public void testInvalidApiKey() {
        try {
            var searcher = new PixabaySearcher("invalid");
            searcher.search("kitten", 10);
            fail();
        } catch (SearcherException e) {
            assertEquals("Encountered HTTP status 400, query kitten", e.getMessage());
        }
    }
}
