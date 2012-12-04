package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.SearcherException;

/**
 * @author Sebastian Sprenger
 * @author David Urbansky
 * 
 */
@Ignore
public class FlickrSearcherTest {

    private static final String TEST_API_KEY = "3bb508b5d6a50a1b30726e5107de7855";

    private FlickrSearcher flickrSearcher;

    @Before
    public void setUp() {
        this.flickrSearcher = new FlickrSearcher(TEST_API_KEY);
    }

    @Test
    public void testSearch() throws SearcherException {
        List<WebImageResult> webImageResults = flickrSearcher.search("cats", "20.12.2010", "dogs,cats", 100, Language.ENGLISH);
        // CollectionHelper.print(webImageResults);
        assertTrue(webImageResults.size() > 0);
    }

}
