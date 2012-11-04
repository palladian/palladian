package ws.palladian.retrieval.feeds.updates;

import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;

public class MavUpdateStrategyTest {
    
    @Test
    public void testMavUpdateStrategy() throws FileNotFoundException, FeedParserException {
        FeedParser parser = new RomeFeedParser();
        Feed feed = parser.getFeed(ResourceHelper.getResourceFile("/feeds/cnn_feed_2012-11-01.rss"));
        FeedPostStatistics fps = new FeedPostStatistics(feed);
        
        MavUpdateStrategy updateStrategy = new MavUpdateStrategy();
        updateStrategy.update(feed, fps, false);
        
        System.out.println(feed.getUpdateInterval());
    }

}
