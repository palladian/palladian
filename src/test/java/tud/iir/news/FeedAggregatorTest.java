package tud.iir.news;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

public class FeedAggregatorTest {

    private static NewsAggregator aggregator;

    @BeforeClass
    public static void before() {
        aggregator = new NewsAggregator(new FeedStoreDummy());
    }

    @Test
    @Ignore
    public void testExceptions() throws FeedAggregatorException {
        aggregator.getEntries("http://feeds.smh.com.au/rssheadlines/top.xml");
        aggregator.getFeedTextType("http://www.carscars.ie/index.php?format=feed&type=atom");
    }

    
    @Test
    public void readFeedFromFile() throws FeedAggregatorException {
        aggregator.setUseScraping(false);
        aggregator.getFeed("data/test/feeds/feed1.xml");
        aggregator.getFeed("data/test/feeds/feed2.xml");
        aggregator.getFeed("http://www.gizmodo.de/feed/atom");
    }

}
