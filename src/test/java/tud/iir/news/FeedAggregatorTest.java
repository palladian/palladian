package tud.iir.news;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.net.URL;
import java.util.List;

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
    
    @Test
    public void testFeedEntryExtraction() throws Exception {
        URL testExample = FeedAggregatorTest.class.getResource("test/feeds/tagesschauExample.xml");
        List<FeedEntry> feedEntries = aggregator.getEntries(testExample.toExternalForm());
        assertNotNull(feedEntries);
        assertFalse(feedEntries.isEmpty());
    }

}
