package tud.iir.news;

import java.util.List;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import tud.iir.helper.CollectionHelper;

public class FeedDatabaseTest {

    private static FeedDatabase db = FeedDatabase.getInstance();

    @BeforeClass
    public static void beforeClass() {
        // clean database before we begin
        db.clearFeedTables();
    }

    @Test
    //@Ignore
    public void testAddFeed() throws FeedAggregatorException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";
        FeedAggregator feedAggregator = new FeedAggregator();
        Feed feed = feedAggregator.getFeed(feedUrl);
        db.addFeed(feed);
    }

    @Test
    @Ignore
    public void testAddDuplicateFeed() throws FeedAggregatorException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";
        FeedAggregator feedAggregator = new FeedAggregator();
        Feed feed = feedAggregator.getFeed(feedUrl);
        System.out.println(db.addFeed(feed));
    }

    @Test
    @Ignore
    public void testAddEntries() throws FeedAggregatorException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";
        FeedAggregator feedAggregator = new FeedAggregator();
        Feed feed = db.getFeedByUrl(feedUrl);
        List<FeedEntry> entries = feedAggregator.getEntries(feedUrl);
        for (FeedEntry entry : entries) {
            db.addEntry(feed, entry);
        }
    }

    @Test
    @Ignore
    public void testGetFeedByUrl() {
        db.getFeedByUrl("http://www.tagesschau.de/xml/rss2");
    }

    @Test
    @Ignore
    public void testGetFeeds() {
        List<Feed> feeds = db.getFeeds();
        CollectionHelper.print(feeds);
    }

    @Test
    public void testAddDuplicateEntry() throws FeedAggregatorException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";

        FeedAggregator aggregator = new FeedAggregator();
        Feed feed = aggregator.getFeed(feedUrl);

        System.out.println(feed);
        db.addFeed(feed);
        System.out.println("added");

        List<FeedEntry> entries = aggregator.getEntries(feedUrl);
        System.out.println(entries);
        FeedEntry firstEntry = entries.iterator().next();
        System.out.println(firstEntry);

        System.out.println(db.addEntry(feed, firstEntry));
        System.out.println(db.addEntry(feed, firstEntry));
        System.out.println(db.addEntry(feed, firstEntry));

    }

    // @Test
    // @Ignore
    // public void testGetEntries() {
    // Feed feed = db.getFeedByUrl("http://www.tagesschau.de/xml/rss2");
    // System.out.println(feed);
    // List<Entry> entries = db.getEntries(feed);
    // CollectionHelper.print(entries);
    // }
    //	
    // @Test
    // @Ignore
    // public void testGetEntries2() {
    // Feed feed = db.getFeedByUrl("http://www.tagesschau.de/xml/rss2");
    // System.out.println(feed);
    // List<Entry> entries = db.getEntries(feed, 5);
    // CollectionHelper.print(entries);
    // }

}
