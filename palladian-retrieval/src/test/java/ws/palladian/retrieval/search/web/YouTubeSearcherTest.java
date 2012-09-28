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
public class YouTubeSearcherTest {

    private YouTubeSearcher searcher;

    @Before
    public void setUp() {
        this.searcher = new YouTubeSearcher();
    }

    @Test
    public void testSearch() throws SearcherException {
        List<WebVideoResult> webImageResults = searcher.search("cats", 100, Language.ENGLISH);
        assertEquals(50, webImageResults.size());

        webImageResults = searcher.search(
                "this might never be found, never ever 83925 2758 082437508234823708472304 02374 0923740923 47", 100,
                Language.ENGLISH);

        assertEquals(0, webImageResults.size());
    }

}
