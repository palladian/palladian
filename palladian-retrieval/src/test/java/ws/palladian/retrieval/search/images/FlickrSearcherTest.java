package ws.palladian.retrieval.search.images;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import ws.palladian.retrieval.search.SearcherException;

public class FlickrSearcherTest {
    @Test
    public void testInvalidApiKey() throws SearcherException {
        try {
            var searcher = new FlickrSearcher("invalid");
            searcher.search("kitten", 10);
            fail();
        } catch (SearcherException e) {
            assertEquals(e.getMessage(), "Invalid API Key (Key has invalid format)");
        }
    }
}
