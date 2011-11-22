package ws.palladian.retrieval.feeds;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import ws.palladian.retrieval.feeds.persistence.CollectionFeedSource;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * 
 * @author Philipp Katz
 *
 */
public class FeedReaderTest {

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
