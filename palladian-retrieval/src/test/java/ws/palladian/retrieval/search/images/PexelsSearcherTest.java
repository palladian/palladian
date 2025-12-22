package ws.palladian.retrieval.search.images;

import org.junit.Ignore;
import org.junit.Test;
import ws.palladian.retrieval.search.SearcherException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class PexelsSearcherTest {
    @Test
    @Ignore // pexels seems to work without proper key too
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
