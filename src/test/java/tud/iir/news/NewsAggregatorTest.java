package tud.iir.news;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import tud.iir.control.AllTests;

public class NewsAggregatorTest {

    private static NewsAggregator aggregator;

    @BeforeClass
    public static void before() {
        aggregator = new NewsAggregator(new FeedStoreDummy());
        aggregator.setDownloadPages(false);
    }

    @Test
    // @Ignore
    public void readFeedFromFile() throws NewsAggregatorException {
        aggregator.downloadFeed("data/test/feeds/feed1.xml");
        aggregator.downloadFeed("data/test/feeds/feed2.xml");
    }

    @Test
    public void downloadFeed() throws NewsAggregatorException {
        if (AllTests.ALL_TESTS) {
            Feed feed = aggregator.downloadFeed("http://www.gizmodo.de/feed/atom");
            // System.out.println(feed);
        }
    }

}
