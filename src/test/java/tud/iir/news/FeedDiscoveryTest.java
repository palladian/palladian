package tud.iir.news;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class FeedDiscoveryTest {

    private static FeedDiscovery feedDiscovery;

    @BeforeClass
    public static void beforeClass() {
        feedDiscovery = new FeedDiscovery();
        // feedDiscovery.setDebugDump(true);
    }

    @Test
    public void testFeedDiscovery() {
        Assert.assertEquals(8, feedDiscovery.getFeedsViaAutodiscovery("data/test/pageContentExtractor/test201.html").size());
        Assert.assertEquals(1, feedDiscovery.getFeedsViaAutodiscovery("data/test/pageContentExtractor/test202.html").size());
        Assert.assertEquals(3, feedDiscovery.getFeedsViaAutodiscovery("data/test/pageContentExtractor/test203.html").size());
        Assert.assertEquals(1, feedDiscovery.getFeedsViaAutodiscovery("data/test/pageContentExtractor/test204.html").size());
        Assert.assertEquals(1, feedDiscovery.getFeedsViaAutodiscovery("data/test/pageContentExtractor/test205.html").size());
        Assert.assertEquals(null, feedDiscovery.getFeedsViaAutodiscovery("data/test/pageContentExtractor/test206.html"));
        Assert.assertEquals(1, feedDiscovery.getFeedsViaAutodiscovery("data/test/pageContentExtractor/test207.html").size());
    }
}