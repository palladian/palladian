package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.SearcherException;

public class EToolsSearcherTest {

    @Test
    public void testEtoolsSearcher() throws SearcherException {
        var searcher = new EToolsSearcher();
        var results = searcher.search("cat", 10, Language.ENGLISH);
        // CollectionHelper.print(results);
        assertEquals(results.size(), 10);
        assertEquals(results.get(0).getUrl(),"https://en.wikipedia.org/wiki/Cat");
        assertEquals(results.get(0).getTitle(),"Cat");
    }

}
