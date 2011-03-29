package ws.palladian.retrieval.feeds;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.retrieval.feeds.meta.MetaInformationCreator;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;


/**
 * <p>
 * This class is responsible for processing the discovered feeds that are saved in the queue.
 * </p>
 * <p>
 * In particular this means, whenever the Importer is called it performs a set of tasks:
 * <ul>
 * <li>Read and filter all discovered feeds from the file.</li>
 * <li>Classify each feed's activity pattern.</li>
 * <li>Classify each feed's language.</li>
 * <li>Classify each feed's format: RSS or Atom.</li>
 * <li>Classify each feed's content type: empty, partial, or full.</li>
 * <li>Delete all entries from the file.</li>
 * </ul>
 * </p>
 * 
 * @author David Urbansky
 * 
 */

public class FeedStoreManager {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedStoreManager.class);

    /** The file where the discovered feeds were written to. */
    private String filePath;

    public FeedStoreManager() {
        try {
            PropertiesConfiguration config = new PropertiesConfiguration("config/feeds.conf");
            filePath = config.getString("crawlerDiscoveryList", FeedDiscoveryCallback.DEFAULT_FILE_PATH);
        } catch (ConfigurationException e) {
            LOGGER.error("error loading configuration: " + e.getMessage());
        }
    }

    public void importFeeds() {

        StopWatch sw = new StopWatch();
        LOGGER.info("start importing feeds");

        FeedImporter na = new FeedImporter(new FeedDatabase());
        na.setStoreItems(false);

        // add feeds which are not present yet
        int numberImported = na.addFeedsFromFile(filePath);

        // empty the feed file
        FileHelper.writeToFile(filePath, "");

        LOGGER.info(numberImported + " feeds were imported in " + sw.getElapsedTimeString());
    }

    public void updateHeaderInformation() {
        StopWatch sw = new StopWatch();
        LOGGER.info("start running the MetaInformationCreator");
        MetaInformationCreator creator = new MetaInformationCreator();
        creator.createMetaInformation();
        LOGGER.info("meta information for all feeds created in " + sw.getElapsedTimeString());
    }

    /**
     * @param args
     * @throws FeedDownloaderException
     */
    public static void main(String[] args) throws FeedDownloaderException {
        // Crawler c = new Crawler();
        // Document document = c.getWebDocument("http://www.newser.com/");

        // NewsAggregator na = new NewsAggregator();
        // Feed f = na.downloadFeed("http://www.buzzfeed.com/index.xml");
        // System.out.println(f.getFormat() + "," + f.getTextType());

    }

}
