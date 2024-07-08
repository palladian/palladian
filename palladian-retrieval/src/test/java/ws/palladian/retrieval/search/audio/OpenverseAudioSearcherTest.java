package ws.palladian.retrieval.search.audio;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearcherException;

public class OpenverseAudioSearcherTest {

    @Test
    public void testOpenverseSearcher() throws SearcherException {
        var openverseSearcher = new OpenverseAudioSearcher();

        var query = new MultifacetQuery.Builder().setText("mozart nachtmusik allegro").setResultCount(10).create();
        var result = openverseSearcher.search(query);
        // CollectionHelper.print(result);
        assertEquals(1, result.getResultList().size());
        assertEquals("Mozart - Eine kleine Nachtmusik - 1. Allegro", result.getResultList().get(0).getTitle());
        assertEquals("https://upload.wikimedia.org/wikipedia/commons/2/24/Mozart_-_Eine_kleine_Nachtmusik_-_1._Allegro.ogg", result.getResultList().get(0).getUrl());
        assertEquals("https://upload.wikimedia.org/wikipedia/commons/2/24/Mozart_-_Eine_kleine_Nachtmusik_-_1._Allegro.ogg", result.getResultList().get(0).getAudioUrl());
        assertEquals(Long.valueOf(1), result.getTotalResultCount());
    }

    @Test
    public void testOpenverseSearcherInvalidKey() throws SearcherException {
        try {
            var openverseSearcher = new OpenverseAudioSearcher("invalid", "invalid");
            openverseSearcher.search("mozart nachtmusik allegro", 10);
            fail();
        } catch (SearcherException e) {
            assertEquals("HTTP status 401 from token endpoint: {\"error\":\"invalid_client\"}", e.getMessage());
        }
    }

}
