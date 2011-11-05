package ws.palladian.retrieval.feeds.discovery;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;

public class FeedDiscoveryTest {

    private static FeedDiscovery feedDiscovery;

    @BeforeClass
    public static void beforeClass() {
        feedDiscovery = new FeedDiscovery();
    }

    @Test
    public void testFeedDiscovery() {

        assertEquals(2, feedDiscovery.discoverFeeds(FeedDiscoveryTest.class.getResource("/pageContentExtractor/test006.html").getFile()).size());
        assertEquals(8, feedDiscovery.discoverFeeds(FeedDiscoveryTest.class.getResource("/pageContentExtractor/test201.html").getFile()).size());
        assertEquals(1, feedDiscovery.discoverFeeds(FeedDiscoveryTest.class.getResource("/pageContentExtractor/test202.html").getFile()).size());
        // cannot test these because URLValidator fails when run offline ...
        // assertEquals(3, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test203.html").size());
        assertEquals(1, feedDiscovery.discoverFeeds(FeedDiscoveryTest.class.getResource("/pageContentExtractor/test204.html").getFile()).size());
        // assertEquals(1, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test205.html").size());
        assertEquals(1, feedDiscovery.discoverFeeds(FeedDiscoveryTest.class.getResource("/pageContentExtractor/test207.html").getFile()).size());

        // page with parse errors; fixed by newer NekoHTML release ...
        // assertEquals(null, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test206.html"));
        // should be: 
        assertEquals(0, feedDiscovery.discoverFeeds(FeedDiscoveryTest.class.getResource("/pageContentExtractor/test206.html").getFile()).size());

        // page with one feed
        List<DiscoveredFeed> feeds = feedDiscovery.discoverFeeds(FeedDiscoveryTest.class.getResource("/pageContentExtractor/test001.html").getFile());
        assertEquals("http://www.tagesschau.de/xml/rss2", feeds.get(0).getFeedLink());

        
        feeds = feedDiscovery.discoverFeeds(FeedDiscoveryTest.class.getResource("/pageContentExtractor/test004.html").getFile());
        assertEquals(3, feeds.size());
        assertEquals("http://www.neustadt-ticker.de/feed/", feeds.get(0).getFeedLink());
        assertEquals("http://www.neustadt-ticker.de/feed/atom/", feeds.get(1).getFeedLink());
        assertEquals("http://www.neustadt-ticker.de/nachrichten/burgerversammlung-in-der-leipziger-vorstadt/feed/", feeds.get(2).getFeedLink());


    }

    @Test
    public void testFeedDiscovery2() {

        // testcase from http://diveintomark.org/archives/2003/12/19/atom-autodiscovery
        // 9 valid Feed-Links pointing to http://www.example.com/xml/atom.xml
        List<DiscoveredFeed> temp = feedDiscovery.discoverFeeds(FeedDiscoveryTest.class.getResource("/pageContentExtractor/test11.html").getFile());
        assertEquals(9, temp.size());
        for (DiscoveredFeed t : temp) {
            assertEquals("http://www.example.com/xml/atom.xml", t.getFeedLink());
        }


    }
}