package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.SearcherException;

/**
 * @author David Urbansky
 * 
 */
@Ignore
public class WebKnoxSearcherTest extends WebSearcherTest {

    private WebKnoxSearcher searcher;
    private WebKnoxNewsSearcher newsSearcher;

    @Before
    public void setUp() {
        this.searcher = new WebKnoxSearcher(getConfig());
        this.newsSearcher = new WebKnoxNewsSearcher(getConfig());
    }

    @Test
    public void testSearch() throws SearcherException {
        List<WebResult> webResults = searcher.search("cats", 10, Language.ENGLISH);
        // CollectionHelper.print(webResults);

        assertTrue(webResults.size() > 0);
    }

    @Test
    public void testSearchNews() throws SearcherException {
        List<WebResult> webResults = newsSearcher.search("obama care", 12, Language.ENGLISH);
        // CollectionHelper.print(webResults);

        assertTrue(webResults.size() > 0);
    }

}
