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
    public void testAddFeed() throws NewsAggregatorException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";
        NewsAggregator newsAggregator = new NewsAggregator();
        Feed feed = newsAggregator.downloadFeed(feedUrl);
        db.addFeed(feed);
    }

    @Test
    @Ignore
    public void testAddDuplicateFeed() throws NewsAggregatorException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";
        NewsAggregator newsAggregator = new NewsAggregator();
        Feed feed = newsAggregator.downloadFeed(feedUrl);
        System.out.println(db.addFeed(feed));
    }

    @Test
    @Ignore
    public void testAddEntries() throws NewsAggregatorException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";
        NewsAggregator newsAggregator = new NewsAggregator();
        Feed feed = db.getFeedByUrl(feedUrl);
        List<FeedEntry> entries = newsAggregator.downloadFeed(feedUrl).getEntries();
        for (FeedEntry entry : entries) {
            db.addFeedEntry(feed, entry);
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
    public void testAddDuplicateEntry() throws NewsAggregatorException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";

        NewsAggregator aggregator = new NewsAggregator();
        Feed feed = aggregator.downloadFeed(feedUrl);

        System.out.println(feed);
        db.addFeed(feed);
        System.out.println("added");

        List<FeedEntry> entries = aggregator.downloadFeed(feedUrl).getEntries();
        System.out.println(entries);
        FeedEntry firstEntry = entries.iterator().next();
        System.out.println(firstEntry);

        System.out.println(db.addFeedEntry(feed, firstEntry));
        System.out.println(db.addFeedEntry(feed, firstEntry));
        System.out.println(db.addFeedEntry(feed, firstEntry));

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
