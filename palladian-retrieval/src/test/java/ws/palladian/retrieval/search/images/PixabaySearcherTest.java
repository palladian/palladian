package ws.palladian.retrieval.search.images;

import org.junit.Test;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.SearcherException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class PixabaySearcherTest {
    @Test
    public void testSafeSearchIsOffByDefault() {
        var searcher = new PixabaySearcher("test-key");
        assertFalse(searcher.isSafeSearch());
        assertFalse(searcher.buildRequest("kitten", 1, 3, Language.ENGLISH).contains("safesearch"));
        assertFalse(searcher.buildVideoRequest("kitten", 1, 3).contains("safesearch"));
    }

    @Test
    public void testSafeSearchAsksForImagesAndVideosSuitableForAllAges() {
        var searcher = new PixabaySearcher("test-key");
        searcher.setSafeSearch(true);
        assertTrue(searcher.isSafeSearch());
        assertEquals("http://pixabay.com/api/?key=test-key&search_term=kitten&image_type=all&page=1&per_page=3&lang=en&safesearch=true",
                searcher.buildRequest("kitten", 1, 3, Language.ENGLISH));
        assertEquals("https://pixabay.com/api/videos/?key=test-key&q=kitten&video_type=all&page=2&per_page=200&safesearch=true",
                searcher.buildVideoRequest("kitten", 2, 200));
    }

    @Test
    public void testInvalidApiKey() {
        try {
            var searcher = new PixabaySearcher("invalid");
            searcher.search("kitten", 10);
            fail();
        } catch (SearcherException e) {
            assertEquals("Encountered HTTP status 400, query kitten", e.getMessage());
        }
    }
}
