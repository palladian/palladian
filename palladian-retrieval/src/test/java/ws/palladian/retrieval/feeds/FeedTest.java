package ws.palladian.retrieval.feeds;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Date;

import org.junit.Test;

public class FeedTest {
    
    @Test
    public void testFeed() {
        // test distinct dates of feed.getLastButOneFeedEntry() and feed.getLastFeedEntry()
        Feed feed = new Feed();
        ArrayList<FeedItem> items = new ArrayList<FeedItem>();
        long baseTime = 1310792400000L;
        Date date = null;

        // add 4 items, dates differ by one hour each
        for (int i = 1; i <= 4; i++) {
            FeedItem item = new FeedItem();
            item.setId(i);
            item.setHash(i + "");
            date = new Date(baseTime + (i * 3600000));
            item.setPublished(date);
            items.add(item);
        }

        // add item equal to last item
        FeedItem item = new FeedItem();
        item.setId(5);
        item.setHash(4 + "");
        date = new Date(baseTime + (4 * 3600000));
        item.setPublished(date);
        items.add(item);

        // add items to feed
        feed.setLastPollTime(new Date());
        feed.setItems(items);
        assertEquals(baseTime + 3 * 3600000, feed.getLastButOneFeedEntry().getTime());
        assertEquals(baseTime + 4 * 3600000, feed.getLastFeedEntry().getTime());

        // add another four items that are older
        items = new ArrayList<FeedItem>();
        for (int i = 1; i <= 4; i++) {
            item = new FeedItem();
            item.setId(i);
            item.setHash(i + "");
            date = new Date(baseTime - (i * 3600000));
            item.setPublished(date);
            items.add(item);
        }

        // add items to feed
        feed.setLastPollTime(new Date());
        feed.setItems(items);
        
        assertEquals(baseTime + 3 * 3600000, feed.getLastButOneFeedEntry().getTime());
        assertEquals(baseTime + 4 * 3600000, feed.getLastFeedEntry().getTime());
    }

}
