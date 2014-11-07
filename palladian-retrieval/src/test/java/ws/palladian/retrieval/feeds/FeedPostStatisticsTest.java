package ws.palladian.retrieval.feeds;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;

import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;

public class FeedPostStatisticsTest {

    @Test
    public void testFeedPostStatistics() throws FeedParserException, FileNotFoundException {
        FeedParser parser = new RomeFeedParser();
        Feed feed = parser.getFeed(ResourceHelper.getResourceFile("/feeds/feed9.xml"));
        FeedPostStatistics fps = new FeedPostStatistics(feed);
        assertEquals(46619608, fps.getAveragePostGap(), 1);
        assertEquals(47. / 24, fps.getAvgEntriesPerDay(), 0.1);
        assertEquals(46, fps.getIntervals().size());
        assertEquals(1478283000, fps.getLongestPostGap());
        assertEquals(2009500, fps.getMedianPostGap());
        // assertEquals(218205144, fps.getPostGapStandardDeviation());
        assertEquals(1277851757000l, fps.getTimeOldestPost());
        assertEquals(1279996259000l, fps.getTimeNewestPost());
        assertEquals(2144502000, fps.getTimeRange());
        assertEquals(24, fps.getTimeRangeInDays());
    }

}
