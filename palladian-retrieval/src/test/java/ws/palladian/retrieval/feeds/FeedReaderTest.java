package ws.palladian.retrieval.feeds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.feeds.persistence.CollectionFeedSource;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.FixUpdateStrategy;

/**
 * <p>
 * Tests whether the feed checker can correctly access feeds.
 * </p>
 * 
 * @author klemens.muthmann@googlemail.com
 * @author Philipp Katz
 * 
 */
public class FeedReaderTest {

    private Collection<Feed> fixture;

    private FeedReader objectOfClassUnderTest;

    @Before
    public void setUp() throws Exception {
        fixture = new HashSet<Feed>();
        fixture.add(new Feed("http://www.tagesschau.de/xml/rss2"));
        objectOfClassUnderTest = new FeedReader(new CollectionFeedSource(fixture));
        objectOfClassUnderTest.setFeedProcessingAction(new DefaultFeedProcessingAction());
        objectOfClassUnderTest.setUpdateStrategy(new FixUpdateStrategy(1));
    }

    /**
     * Test method for {@link tud.iir.news.FeedReader#startContinuousReading()}.
     */
    @Test
    @Ignore
    public void testContinuousReading() {
        // objectOfClassUnderTest.startContinuousReading(10 * DateHelper.SECOND_MS);
        objectOfClassUnderTest.startContinuousReading(TimeUnit.SECONDS.toMillis(180));
        objectOfClassUnderTest.stopContinuousReading();
    }

    @Test
    public void testSynchronizeWithStore() {

        Feed f1 = new Feed("http://example.com/feed1");
        Feed f2 = new Feed("http://example.com/feed2");
        Feed f3 = new Feed("http://example.com/feed3");
        Feed f4 = new Feed("http://example.com/feed4");
        Feed f5 = new Feed("http://example.com/feed5");

        FeedStore feedStore = new CollectionFeedSource();
        feedStore.addFeed(f1);
        feedStore.addFeed(f2);
        feedStore.addFeed(f3);

        FeedReader feedReader = new FeedReader(feedStore);
        assertEquals(3, feedReader.getFeeds().size());

        feedStore.addFeed(f4);
        feedStore.addFeed(f5);
        feedStore.deleteFeedByUrl("http://example.com/feed1");

        int delta = feedReader.synchronizeWithStore();
        assertEquals(1, delta);

        assertEquals(4, feedReader.getFeeds().size());

        assertTrue(feedReader.getFeeds().contains(f2));
        assertTrue(feedReader.getFeeds().contains(f3));
        assertTrue(feedReader.getFeeds().contains(f4));
        assertTrue(feedReader.getFeeds().contains(f5));

    }

}
