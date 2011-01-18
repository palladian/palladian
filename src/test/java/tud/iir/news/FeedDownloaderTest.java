package tud.iir.news;

import org.junit.BeforeClass;
import org.junit.Test;

import tud.iir.control.AllTests;

public class FeedDownloaderTest {

    private static FeedDownloader aggregator;

    @BeforeClass
    public static void before() {
        aggregator = new FeedDownloader();
    }

    @Test
    // @Ignore
    public void readFeedFromFile() throws NewsAggregatorException {
        aggregator.getFeed("data/test/feeds/feed1.xml");
        aggregator.getFeed("data/test/feeds/feed2.xml");
    }

    @Test
    public void downloadFeed() throws NewsAggregatorException {
        if (AllTests.ALL_TESTS) {
            Feed feed = aggregator.getFeed("http://www.gizmodo.de/feed/atom");
            // System.out.println(feed);
        }
    }

}
