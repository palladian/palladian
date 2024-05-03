package ws.palladian.retrieval.search.audio;

import static org.junit.Assert.assertEquals;

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
        assertEquals(Long.valueOf(1), result.getResultCount());
    }

}
