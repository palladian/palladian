package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;

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
public class TwitterSearcherTest extends WebSearcherTest {

    private TwitterSearcher searcher;

    @Before
    public void setUp() {
        this.searcher = new TwitterSearcher();
    }

    @Test
    public void testSearch() throws SearcherException {
        List<WebResult> webResults = searcher.search("cats", 10, Language.ENGLISH);
        // CollectionHelper.print(webResults);
        assertEquals(10, webResults.size());

        webResults = searcher.search("cats that you may never find 82349 23984 203498", 10, Language.ENGLISH);
        // CollectionHelper.print(webResults);

        assertEquals(0, webResults.size());
    }

}
