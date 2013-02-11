package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import ws.palladian.retrieval.search.news.NewsSeecrSearcher;

public class NewsSeecrSearcherTest {

    @Test
    public void testParseDate() {
        assertNotNull(NewsSeecrSearcher.parseDate("2013-01-10T00:01:00.000+0000"));
    }

}
