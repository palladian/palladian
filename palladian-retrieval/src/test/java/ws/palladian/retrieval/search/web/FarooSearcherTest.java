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
public class FarooSearcherTest extends WebSearcherTest {

    private FarooSearcher searcher;
    private FarooNewsSearcher newsSearcher;

    @Before
    public void setUp() {
        this.searcher = new FarooSearcher();
        this.newsSearcher = new FarooNewsSearcher();
    }

    @Test
    public void testSearch() throws SearcherException {
        List<WebResult> webResults = searcher.search("cats", 10, Language.ENGLISH);
        // CollectionHelper.print(webResults);

        assertEquals(10, webResults.size());

        webResults = searcher.search("cats 8 932 4238 429384", 10, Language.ENGLISH);
        // CollectionHelper.print(webResults);

        assertEquals(0, webResults.size());
    }

    @Test
    public void testSearchNews() throws SearcherException {
        List<WebResult> webResults = newsSearcher.search("obama", 12, Language.ENGLISH);
        // CollectionHelper.print(webResults);

        assertEquals(12, webResults.size());
    }

}
