package ws.palladian.retrieval.feeds.discovery;

import java.util.List;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.RetrieverCallback;

/**
 * <p>This class is used as a callback to automatically detect news feeds on pages which are downloaded with the
 * {@link DocumentRetriever}. Discovered feed URLs are written into a text file. This is singleton as we have
 * potentially multiple DocumentRetriever instances, but writing to the list must be coordinated. See feeds.conf for
 * options concerning the discovery.</p>
 * 
 * @author Philipp Katz
 * 
 */
public class FeedDiscoveryCallback implements RetrieverCallback<Document> {

    /** The singleton. */
    private static final FeedDiscoveryCallback INSTANCE = new FeedDiscoveryCallback();

    /** The class logger. */
    // private static final Logger LOGGER = Logger.getLogger(FeedDiscoveryCallback.class);

    /** The default file where the discovered feeds are written to. */
    public static final String DEFAULT_FILE_PATH = "data/discovered_feeds.txt";

    /** Instance of FeedDiscovery to which we delegate for discovery. */
    private FeedDiscovery feedDiscovery = new FeedDiscovery();

    /** The file where the discovered feeds are written to. */
    private String filePath = DEFAULT_FILE_PATH;

    private FeedDiscoveryCallback() {
        Logger.getRootLogger().trace("FeedDiscoveryCallback.<init>");
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        if (config != null) {
            filePath = config.getString("feedDiscovery.crawlerDiscoveryList", DEFAULT_FILE_PATH);
        } else {
            Logger.getRootLogger().warn("could not load configuration, use defaults");
        }
    }

    /**
     * 
     * @return Singleton of {@link FeedDiscoveryCallback} which is shared among all {@link DocumentRetriever} instances.
     */
    public static FeedDiscoveryCallback getInstance() {
        return INSTANCE;
    }

    @Override
    public void onFinishRetrieval(Document document) {
        if (document != null) {
            List<DiscoveredFeed> feeds = feedDiscovery.discoverFeeds(document);
            for (DiscoveredFeed feed : feeds) {
                // output to the file must be synced, or we will lose data when
                // writing from multiple crawl threads
                synchronized (this) {
                    FileHelper.appendLineIfNotPresent(filePath, feed.toCsv());
                }
            }
        }
    }

    public static void main(String[] args) {
        // FeedDiscoveryCallback.getInstance();
        // Crawler c = new Crawler();
        // c.getDocumentRetriever().setFeedAutodiscovery(true);
        // c.setStopCount(1000000);
        // c.startCrawl("http://www.dmoz.org/", false, true);
    }

}
