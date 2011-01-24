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
    public void testAddFeed() throws FeedDownloaderException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";
        FeedDownloader newsAggregator = new FeedDownloader();
        Feed feed = newsAggregator.getFeed(feedUrl);
        db.addFeed(feed);
    }

    @Test
    @Ignore
    public void testAddDuplicateFeed() throws FeedDownloaderException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";
        FeedDownloader feedDownloader = new FeedDownloader();
        Feed feed = feedDownloader.getFeed(feedUrl);
        System.out.println(db.addFeed(feed));
    }

    @Test
    @Ignore
    public void testAddEntries() throws FeedDownloaderException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";
        FeedDownloader newsAggregator = new FeedDownloader();
        Feed feed = db.getFeedByUrl(feedUrl);
        List<FeedItem> entries = newsAggregator.getFeed(feedUrl).getItems();
        for (FeedItem entry : entries) {
            db.addFeedItem(feed, entry);
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
    public void testAddDuplicateEntry() throws FeedDownloaderException {
        String feedUrl = "http://www.tagesschau.de/xml/rss2";

        FeedDownloader aggregator = new FeedDownloader();
        Feed feed = aggregator.getFeed(feedUrl);

        System.out.println(feed);
        db.addFeed(feed);
        System.out.println("added");

        List<FeedItem> entries = aggregator.getFeed(feedUrl).getItems();
        System.out.println(entries);
        FeedItem firstEntry = entries.iterator().next();
        System.out.println(firstEntry);

        System.out.println(db.addFeedItem(feed, firstEntry));
        System.out.println(db.addFeedItem(feed, firstEntry));
        System.out.println(db.addFeedItem(feed, firstEntry));

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
