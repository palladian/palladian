package ws.palladian.retrieval.feeds;

import java.io.File;
import java.util.List;

import org.apache.commons.configuration.Configuration;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.feeds.discovery.FeedDiscovery;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.FeedParserException;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.search.web.GoogleSearcher;

/**
 * <p>
 * Example class illustrating usage of most important feed classes.
 * </p>
 * 
 * @author Philipp Katz
 */
public class UsageExamples {

    public static void main(String[] args) throws FeedParserException {
        // search feeds for "Porsche 911"
        File discoveredFeedsFile = new File("data/foundFeeds.txt");
        FeedDiscovery feedDiscovery = new FeedDiscovery(new GoogleSearcher(), discoveredFeedsFile, 10, 100, false);
        feedDiscovery.addQuery("Porsche 911");
        feedDiscovery.findFeeds();

        // download a feed
        FeedParser feedParser = new RomeFeedParser();
        Feed feed = feedParser.getFeed("http://rss.cnn.com/rss/edition.rss");
        List<FeedItem> feedItems = feed.getItems();
        CollectionHelper.print(feedItems);

        // initialize the FeedDatabase for storing the data
        Configuration config = ConfigHolder.getInstance().getConfig();
        final FeedStore feedStore = DatabaseManagerFactory.create(FeedDatabase.class, config);

        // add some feed URLs to the database
//        FeedImporter feedImporter = new FeedImporter(feedStore, true, true);
//        feedImporter.addFeedsFromFile(discoveredFeedsFile.getPath(), 10);

        // specify what to do, when feed contains new items; here we simply add them to the database
        FeedProcessingAction feedProcessingAction = new DefaultFeedProcessingAction() {
            @Override
            public void onModified(Feed feed, HttpResult httpResult) {
                List<FeedItem> items = feed.getNewItems();
                feedStore.addFeedItems(items);
            }
        };
        // start reading feeds
        FeedReaderSettings.Builder settingsBuilder = new FeedReaderSettings.Builder();
        settingsBuilder.setStore(feedStore);
        settingsBuilder.setAction(feedProcessingAction);
        FeedReader feedReader = new FeedReader(settingsBuilder.create());
        feedReader.start();

    }

}
