package ws.palladian.retrieval.search.videos;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import ws.palladian.retrieval.search.SearcherException;

public class VimeoSearcherTest {

    @Test
    public void testInvalidCredentials() throws SearcherException {
        try {
            var searcher = new VimeoSearcher("invalid");
            searcher.search("kitten", 10);
            fail();
        } catch (SearcherException e) {
            assertEquals(e.getMessage(), "Something strange occurred. Please get in touch with the app's creator.");
        }
    }

}
