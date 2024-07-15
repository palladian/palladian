package ws.palladian.retrieval.search.images;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import ws.palladian.retrieval.search.SearcherException;

public class PexelsSearcherTest {
    @Test
    public void testInvalidApiKey() throws SearcherException {
        try {
            var searcher = new PexelsSearcher("invalid");
            searcher.search("kitten", 10);
            fail();
        } catch (SearcherException e) {
            assertEquals(e.getMessage(), "Encountered HTTP status 401");
        }
    }
}
