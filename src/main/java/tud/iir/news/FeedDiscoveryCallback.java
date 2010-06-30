package tud.iir.news;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;

import tud.iir.helper.FileHelper;
import tud.iir.web.Crawler;
import tud.iir.web.CrawlerCallback;

/**
 * This class is used as a callback to automatically detect news feeds on pages which are downloaded with the {@link Crawler}. Discovered feed URLs are written
 * into a text file. This is singleton as we have potentially multiple Crawler instances, but writing to the list must be coordinated.
 * 
 * @author Philipp Katz
 * 
 */
public class FeedDiscoveryCallback implements CrawlerCallback {

    private static final Logger logger = Logger.getLogger(FeedDiscoveryCallback.class);

    private static final String DEFAULT_FILE_PATH = "data/discovered_feeds.txt";

    private static final FeedDiscoveryCallback INSTANCE = new FeedDiscoveryCallback();

    private FeedDiscovery feedDiscovery = new FeedDiscovery();

    private String filePath = DEFAULT_FILE_PATH;

    private FeedDiscoveryCallback() {
        logger.trace("<init>");
        try {
            PropertiesConfiguration config = new PropertiesConfiguration("config/feeds.conf");
            filePath = config.getString("crawlerDiscoveryList", DEFAULT_FILE_PATH);
        } catch (ConfigurationException e) {
            logger.trace("failed to read configuration " + e.getMessage());
        }
    }

    /**
     * 
     * @return Singleton of {@link FeedDiscoveryCallback} which is shared among all {@link Crawler} instances.
     */
    public static FeedDiscoveryCallback getInstance() {
        return INSTANCE;
    }

    @Override
    public void crawlerCallback(Document document) {
        if (document != null) {
            List<String> feeds = feedDiscovery.getFeedsViaAutodiscovery(document);
            for (String feed : feeds) {
                // output to the file must be synched, or we will lose data when
                // writing from multiple crawl threads
                synchronized (this) {
                    appendToFileIfNotPresent(filePath, feed, false);
                }
            }
        }
    }

    /**
     * Appends a line to the specified text file if it does not already exist.
     * 
     * @param filePath
     * @param string
     * @param before
     */
    private static void appendToFileIfNotPresent(String filePath, String string, boolean before) {
        boolean add = true;
        // scan the file, if the line is already present ...
        ArrayList<String> lines = FileHelper.readFileToArray(filePath);
        for (String line : lines) {
            if (line.equals(string)) {
                add = false;
                break;
            }
        }
        if (add) {
            FileHelper.appendToFile(filePath, string, before);
        }
    }

}
