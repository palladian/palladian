package ws.palladian.retrieval.feeds;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import ws.palladian.helper.io.ResourceHelper;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;

public class FeedClassifierTest {

    private FeedParser feedParser;
    private Date imaginaryDate;

    @Before
    public void setUp() throws ParseException {
        feedParser = new RomeFeedParser();
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z", Locale.US);
        imaginaryDate = dateFormat.parse("2010-06-13 12:00:00.000 UTC");
    }

    /**
     * XXX rethink how to test this, not trivial since time in feed is fixed and classification algorithm uses time
     * differences which change at execution
     * can not test this since it depends on current time (works though)
     * 
     * @throws FeedParserException
     * @throws FileNotFoundException
     * @throws ParseException
     */
    @Test
    public void testFeedClassification() throws FileNotFoundException, FeedParserException, ParseException {

        // http://beta.rottentomatoes.com/syndication/rss/upcoming.xml
        Feed feed = feedParser.getFeed(ResourceHelper.getResourceFile("/feeds/feed1.xml"));
        feed.setLastPollTime(imaginaryDate);
        FeedActivityPattern feedClass = FeedClassifier.classify(feed);
        assertEquals(FeedActivityPattern.CLASS_ON_THE_FLY, feedClass);

        // http://29content6.com/nipponsport/NIPPONSPORTpodcast.xml
        feed = feedParser.getFeed(ResourceHelper.getResourceFile("/feeds/feed2.xml"));
        feed.setLastPollTime(imaginaryDate);
        feedClass = FeedClassifier.classify(feed);
        assertEquals(FeedActivityPattern.CLASS_SPONTANEOUS, feedClass);

        // http://beta.rottentomatoes.com/syndication/rss/upcoming.xml (set back to older date)
        feed = feedParser.getFeed(ResourceHelper.getResourceFile("/feeds/feed3.xml"));
        feed.setLastPollTime(imaginaryDate);
        feedClass = FeedClassifier.classify(feed);
        assertEquals(FeedActivityPattern.CLASS_CHUNKED, feedClass);

        // http://www.amazingwomenbbs.com/amazingwomen.rss
        feed = feedParser.getFeed(ResourceHelper.getResourceFile("/feeds/feed4.xml"));
        feed.setLastPollTime(imaginaryDate);
        feedClass = FeedClassifier.classify(feed);
        assertEquals(FeedActivityPattern.CLASS_ZOMBIE, feedClass);

        // http://feeds.nydailynews.com/nydnrss/news
        feed = feedParser.getFeed(ResourceHelper.getResourceFile("/feeds/feed5.xml"));
        feed.setLastPollTime(imaginaryDate);
        feedClass = FeedClassifier.classify(feed);
        assertEquals(FeedActivityPattern.CLASS_SLICED, feedClass);

        // http://feeds.gawker.com/lifehacker/full
        feed = feedParser.getFeed(ResourceHelper.getResourceFile("/feeds/feed6.xml"));
        feed.setLastPollTime(imaginaryDate);
        feedClass = FeedClassifier.classify(feed);
        assertEquals(FeedActivityPattern.CLASS_SLICED, feedClass);

        // http://www.reddit.com/new/.rssl
        feed = feedParser.getFeed(ResourceHelper.getResourceFile("/feeds/feed7.xml"));
        feed.setLastPollTime(imaginaryDate);
        feedClass = FeedClassifier.classify(feed);
        assertEquals(FeedActivityPattern.CLASS_SLICED, feedClass); // was constant
    }

}