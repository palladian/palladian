package ws.palladian.retrieval.feeds;

import java.util.Collection;
import java.util.List;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.SourceRetrieverManager;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * Example class illustrating usage of most important feed classes.
 * 
 * @author Philipp Katz
 *
 */
public class FeedsExamples {
    
    public static void main(String[] args) throws FeedDownloaderException {

        // search feeds for "Porsche 911"
        FeedDiscovery feedDiscovery = new FeedDiscovery();
        feedDiscovery.setSearchEngine(SourceRetrieverManager.YAHOO_BOSS);
        feedDiscovery.addQuery("Porsche 911");
        feedDiscovery.setResultLimit(100);
        feedDiscovery.findFeeds();
        Collection<String> feedUrls = feedDiscovery.getFeeds();
        CollectionHelper.print(feedUrls);
        
        // download a feed
        FeedDownloader feedDownloader = new FeedDownloader();
        Feed feed = feedDownloader.getFeed("http://rss.cnn.com/rss/edition.rss");
        List<FeedItem> feedItems = feed.getItems();
        CollectionHelper.print(feedItems);
        
        // initialize the FeedDatabase for storing the data
        FeedStore feedStore = new FeedDatabase();
        
        // add some feed URLs to the database
        FeedImporter feedImporter = new FeedImporter(feedStore);
        feedImporter.addFeeds(feedUrls);
        
        // start aggregating news for the feeds in the database
        FeedReader feedReader = new FeedReader(feedStore);
        feedReader.aggregate(false);
        
        
    }

}
