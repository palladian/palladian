package tud.iir.web.feeds;

import org.junit.BeforeClass;
import org.junit.Test;

import tud.iir.control.AllTests;
import tud.iir.web.feeds.Feed;
import tud.iir.web.feeds.FeedDownloader;
import tud.iir.web.feeds.FeedDownloaderException;

public class FeedDownloaderTest {

    private static FeedDownloader aggregator;

    @BeforeClass
    public static void before() {
        aggregator = new FeedDownloader();
    }

    @Test
    // @Ignore
    public void readFeedFromFile() throws FeedDownloaderException {
        aggregator.getFeed("data/test/feeds/feed1.xml");
        aggregator.getFeed("data/test/feeds/feed2.xml");
    }

    @Test
    public void downloadFeed() throws FeedDownloaderException {
        if (AllTests.ALL_TESTS) {
            Feed feed = aggregator.getFeed("http://www.gizmodo.de/feed/atom");
            // System.out.println(feed);
        }
    }

}
