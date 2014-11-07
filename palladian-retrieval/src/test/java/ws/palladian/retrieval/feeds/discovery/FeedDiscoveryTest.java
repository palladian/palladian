package ws.palladian.retrieval.feeds.discovery;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;

public class FeedDiscoveryTest {

    // FIXME enable again
    @Test
    @Ignore
    public void testFeedDiscovery() throws FileNotFoundException {

        assertEquals(2, FeedDiscovery.discoverFeeds(ResourceHelper.getResourceFile("/pageContentExtractor/test006.html")).size());
        assertEquals(8, FeedDiscovery.discoverFeeds(ResourceHelper.getResourceFile("/pageContentExtractor/test201.html")).size());
        assertEquals(1, FeedDiscovery.discoverFeeds(ResourceHelper.getResourceFile("/pageContentExtractor/test202.html")).size());
        // cannot test these because URLValidator fails when run offline ...
        // assertEquals(3, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test203.html").size());
        assertEquals(1, FeedDiscovery.discoverFeeds(ResourceHelper.getResourceFile("/pageContentExtractor/test204.html")).size());
        // assertEquals(1, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test205.html").size());
        assertEquals(1, FeedDiscovery.discoverFeeds(ResourceHelper.getResourceFile("/pageContentExtractor/test207.html")).size());

        // page with parse errors; fixed by newer NekoHTML release ...
        // assertEquals(null, feedDiscovery.discoverFeeds("data/test/pageContentExtractor/test206.html"));
        // should be: 
        assertEquals(0, FeedDiscovery.discoverFeeds(ResourceHelper.getResourceFile("/pageContentExtractor/test206.html")).size());

        // page with one feed
        List<DiscoveredFeed> feeds = FeedDiscovery.discoverFeeds(ResourceHelper.getResourceFile("/pageContentExtractor/test001.html"));
        assertEquals("http://www.tagesschau.de/xml/rss2", feeds.get(0).getFeedLink());

        
        feeds = FeedDiscovery.discoverFeeds(ResourceHelper.getResourceFile("/pageContentExtractor/test004.html"));
        assertEquals(3, feeds.size());
        assertEquals("http://www.neustadt-ticker.de/feed/", feeds.get(0).getFeedLink());
        assertEquals("http://www.neustadt-ticker.de/feed/atom/", feeds.get(1).getFeedLink());
        assertEquals("http://www.neustadt-ticker.de/nachrichten/burgerversammlung-in-der-leipziger-vorstadt/feed/", feeds.get(2).getFeedLink());


    }

    @Test
    public void testFeedDiscovery2() throws FileNotFoundException {

        // testcase from http://diveintomark.org/archives/2003/12/19/atom-autodiscovery
        // 9 valid Feed-Links pointing to http://www.example.com/xml/atom.xml
        List<DiscoveredFeed> temp = FeedDiscovery.discoverFeeds(ResourceHelper.getResourceFile("/feedDiscoveryPage.html"));
        assertEquals(9, temp.size());
        for (DiscoveredFeed t : temp) {
            assertEquals("http://www.example.com/xml/atom.xml", t.getFeedLink());
        }


    }
}