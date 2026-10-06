package ws.palladian.retrieval.search.images;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import ws.palladian.retrieval.search.SearcherException;

public class UnsplashSearcherTest {
    @Test
    public void testDefaultContentFilterIsUnsplashsOwn() {
        var searcher = new UnsplashSearcher("test-key");
        assertFalse(searcher.isSafeSearch());
        assertEquals("https://api.unsplash.com/search/photos?query=kitten&per_page=30&page=1", searcher.buildRequest("kitten", 1, 30, null));
    }

    @Test
    public void testSafeSearchUsesTheHighContentFilter() {
        var searcher = new UnsplashSearcher("test-key");
        searcher.setSafeSearch(true);
        assertTrue(searcher.isSafeSearch());
        assertEquals("https://api.unsplash.com/search/photos?query=kitten&per_page=30&page=1&content_filter=high", searcher.buildRequest("kitten", 1, 30, null));
        assertEquals("https://api.unsplash.com/search/photos?query=kitten&per_page=30&page=1&orientation=landscape&content_filter=high",
                searcher.buildRequest("kitten", 1, 30, Orientation.LANDSCAPE));
    }

    @Test
    public void testInvalidApiKey() throws SearcherException {
        try {
            var searcher = new UnsplashSearcher("invalid");
            searcher.search("kitten", 10);
            fail();
        } catch (SearcherException e) {
            assertEquals(e.getMessage(), "Encountered HTTP status 401");
        }
    }
}
