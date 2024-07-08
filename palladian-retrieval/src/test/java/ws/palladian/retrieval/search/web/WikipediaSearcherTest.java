package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.MultifacetQuery;
import ws.palladian.retrieval.search.SearcherException;

public class WikipediaSearcherTest {
    @Test
    public void testSearch() throws SearcherException {
        var searcher = new WikipediaSearcher();
        var result = searcher.search(new MultifacetQuery.Builder().setText("Dresden").setResultCount(75).setLanguage(Language.GERMAN).create());
        assertEquals(75, result.getResultList().size());
        assertEquals("Dresden", result.getResultList().get(0).getTitle());
        assertEquals("https://de.wikipedia.org/wiki/Dresden", result.getResultList().get(0).getUrl());
        assertTrue(result.getResultCount() > 50_000);
    }

    @Test
    public void unescapeSummary() throws SearcherException {
        var searcher = new WikipediaSearcher();
        var result = searcher.search("AT&T", 1);
        assertTrue(result.get(0).getSummary().startsWith(
                "AT&T Inc. is an American multinational telecommunications holding company headquartered at Whitacre Tower in Downtown Dallas, Texas."));
    }
}
