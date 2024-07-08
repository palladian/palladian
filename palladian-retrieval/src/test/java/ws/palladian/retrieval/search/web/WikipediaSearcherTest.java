package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.SearcherException;

public class WikipediaSearcherTest {
    @Test
    public void testSearch() throws SearcherException {
        var searcher = new WikipediaSearcher();
        var result = searcher.search("Dresden", 75, Language.GERMAN);
        assertEquals(75, result.size());
        assertEquals("Dresden", result.get(0).getTitle());
        assertEquals("https://de.wikipedia.org/wiki/Dresden", result.get(0).getUrl());
    }

    @Test
    public void unescapeSummary() throws SearcherException {
        var searcher = new WikipediaSearcher();
        var result = searcher.search("AT&T", 1);
        assertTrue(result.get(0).getSummary().startsWith(
                "AT&T Inc. is an American multinational telecommunications holding company headquartered at Whitacre Tower in Downtown Dallas, Texas."));
    }
}
