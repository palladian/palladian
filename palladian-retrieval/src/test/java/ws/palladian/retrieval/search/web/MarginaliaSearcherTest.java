package ws.palladian.retrieval.search.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.search.SearcherException;

@Ignore
public class MarginaliaSearcherTest {
    @Test
    public void testMarginalia() throws SearcherException {
        var key = "public"; // public key usually doesn't work - get your own
        var marginalia = new MarginaliaSearcher(key);
        var results = marginalia.search("json api", 100);
        CollectionHelper.print(results);
    }

    @Test
    public void testMarginaliaInvalidLicenseKey() {
        try {
            var marginalia = new MarginaliaSearcher("invalid");
            marginalia.search("json api", 10);
            fail("Expected test to fail due to invalid license key");
        } catch (SearcherException e) {
            assertEquals("HTTP status 401: Invalid license key", e.getMessage());
        }
    }
}
