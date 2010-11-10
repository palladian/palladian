package tud.iir.news;

import java.util.List;

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

        feedDiscovery.setOnlyPreferred(false);
        Assert.assertEquals(8, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test201.html").size());
        Assert.assertEquals(1, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test202.html").size());
        // cannot test these because URLValidaor fails when run offline ...
        // Assert.assertEquals(3, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test203.html").size());
        Assert.assertEquals(1, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test204.html").size());
        // Assert.assertEquals(1, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test205.html").size());
        Assert.assertEquals(1, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test207.html").size());

        // page with parse errors; fixed by newer NekoHTML release ...
        // Assert.assertEquals(null, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test206.html"));
        // should be: 
        Assert.assertEquals(0, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test206.html").size());

        // page with one feed
        Assert.assertEquals("http://www.tagesschau.de/xml/rss2", feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test001.html").get(0));

        // page with one RSS and one Atom feed -- we want just the Atom feed
        List<String> temp = feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test004.html");
        Assert.assertEquals(1, temp.size());
        Assert.assertEquals("http://www.neustadt-ticker.de/feed/atom/", temp.get(0));

        // get the first, preferred feed
        feedDiscovery.setOnlyPreferred(true);
        temp = feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test006.html");
        Assert.assertEquals(1, temp.size());
        Assert.assertEquals("http://www.wired.com/gadgetlab/feed/", temp.get(0));

    }

    @Test
    public void testFeedDiscovery2() {

        feedDiscovery.setOnlyPreferred(false);

        // testcase from http://diveintomark.org/archives/2003/12/19/atom-autodiscovery
        // 9 valid Feed-Links pointing to http://www.example.com/xml/atom.xml
        List<String> temp = feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test11.html");
        Assert.assertEquals(9, temp.size());
        for (String t : temp) {
            Assert.assertEquals("http://www.example.com/xml/atom.xml", t);
        }


    }
}