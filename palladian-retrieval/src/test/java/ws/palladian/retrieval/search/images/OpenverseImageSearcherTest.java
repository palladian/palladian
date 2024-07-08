package ws.palladian.retrieval.search.images;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearcherException;

public class OpenverseImageSearcherTest {

    @Test
    public void testOpenverseSearcher() throws SearcherException {
        var openverseSearcher = new OpenverseImageSearcher();

        var query = new MultifacetQuery.Builder().setText("paul klee offrande").setResultCount(10).create();
        var result = openverseSearcher.search(query);
        // CollectionHelper.print(result);
        assertEquals(1, result.getResultList().size());
        assertEquals("Offrande fanée (1937) - Paul Klee (1879 - 1940)", result.getResultList().get(0).getTitle());
        assertEquals("https://live.staticflickr.com/65535/25711504863_6356836710_b.jpg", result.getResultList().get(0).getUrl());
        assertEquals("https://live.staticflickr.com/65535/25711504863_6356836710_b.jpg", result.getResultList().get(0).getImageUrl());
        assertEquals(Long.valueOf(1), result.getTotalResultCount());
    }

    @Test
    public void testOpenverseSearcherInvalidKey() throws SearcherException {
        try {
            var openverseSearcher = new OpenverseImageSearcher("invalid", "invalid");
            openverseSearcher.search("paul klee", 10);
            fail();
        } catch (SearcherException e) {
            assertEquals("HTTP status 401 from token endpoint: {\"error\":\"invalid_client\"}", e.getMessage());
        }
    }
}
