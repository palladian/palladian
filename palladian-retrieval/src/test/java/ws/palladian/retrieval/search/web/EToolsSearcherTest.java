package ws.palladian.retrieval.search.web;

import org.junit.Test;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.SearcherException;

import static org.junit.Assert.assertEquals;

public class EToolsSearcherTest {

    @Test
    public void testEtoolsSearcher() throws SearcherException {
        var searcher = new EToolsSearcher();
        var results = searcher.search("cat", 10, Language.ENGLISH);
        // CollectionHelper.print(results);
        assertEquals(10, results.size());
        assertEquals("https://en.wikipedia.org/wiki/Cat", results.get(0).getUrl());
        //        assertEquals("Cat", results.get(0).getTitle().replace(" - Wikipedia", ""));
    }

}
