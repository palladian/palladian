package ws.palladian.retrieval.search.socialmedia;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

// import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.constants.Language;
import ws.palladian.retrieval.search.SearcherException;

public class BlueskySearcherTest {

    @Test
    public void testSearch() throws SearcherException {
        var searcher = new BlueskySearcher();
        var results = searcher.search("winter", 15, Language.GERMAN);
        // CollectionHelper.print(results);
        assertEquals(15, results.size());
    }

    @Test
    public void testConvertAtUriToUrl() {
        assertEquals("https://bsky.app/profile/did:plc:mnnxdrxw3wncocae53mmdsxi/post/3latlue4glc26", BlueskySearcher
                .convertAtUriToUrl("at://did:plc:mnnxdrxw3wncocae53mmdsxi/app.bsky.feed.post/3latlue4glc26"));
    }

}