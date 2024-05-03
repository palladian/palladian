package ws.palladian.retrieval.search.images;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearcherException;

public class OpenverseSearcherTest {

    @Test
    public void testOpenverseSearcher() throws SearcherException {
        var openverseSearcher = new OpenverseSearcher();

        var query = new MultifacetQuery.Builder().setText("paul klee offrande").setResultCount(10).create();
        var result = openverseSearcher.search(query);
        // CollectionHelper.print(result);
        assertEquals(1, result.getResultList().size());
        assertEquals("Offrande fanée (1937) - Paul Klee (1879 - 1940)", result.getResultList().get(0).getTitle());
        assertEquals("https://live.staticflickr.com/65535/25711504863_6356836710_b.jpg", result.getResultList().get(0).getUrl());
        assertEquals("https://live.staticflickr.com/65535/25711504863_6356836710_b.jpg", result.getResultList().get(0).getImageUrl());
        assertEquals(Long.valueOf(1), result.getResultCount());
    }

}
