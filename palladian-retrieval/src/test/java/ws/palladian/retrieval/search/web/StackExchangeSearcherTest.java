package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

// import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.search.SearcherException;

public class StackExchangeSearcherTest {

    @Test
    public void testInvalidSite() throws SearcherException {
        try {
            var searcher = new StackExchangeSearcher("invalid");
            searcher.search("java", 10);
            fail();
        } catch (SearcherException e) {
            assertTrue(e.getMessage().startsWith("Encountered HTTP status 400 while accessing"));
        }
    }

    @Test
    public void testSearch() throws SearcherException {
        var searcher = new StackExchangeSearcher("stackoverflow.com");
        var results = searcher.search("java", 10);
        // CollectionHelper.print(results);
        assertEquals(10, results.size());

        var results2 = searcher.search("java", 125);
        assertEquals(125, results2.size());

        // var results3 = searcher.search("async await in Angular ngOnInit", 1);
        // CollectionHelper.print(results3);
    }

}
