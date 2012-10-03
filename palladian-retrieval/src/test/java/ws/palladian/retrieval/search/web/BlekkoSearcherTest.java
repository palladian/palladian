package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.SearcherException;

/**
 * @author David Urbansky
 * 
 */
public class BlekkoSearcherTest extends WebSearcherTest {

    private BlekkoSearcher searcher;

    @Before
    public void setUp() {
        this.searcher = new BlekkoSearcher();
    }

    @Test
    public void testSearch() throws SearcherException {
        List<WebResult> webResults = searcher.search("cats", 15, Language.ENGLISH);
        // CollectionHelper.print(webResults);
        assertEquals(15, webResults.size());

        webResults = searcher.search("cats that you may never find #5u 23io -is -the", 10,
                Language.ENGLISH);
        // CollectionHelper.print(webResults);

        assertEquals(0, webResults.size());
    }

}
