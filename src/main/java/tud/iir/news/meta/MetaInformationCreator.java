/**
 * Created on: 28.07.2010 17:43:02
 */
package tud.iir.news.meta;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import tud.iir.news.Feed;
import tud.iir.news.FeedDatabase;
import tud.iir.news.FeedStore;

/**
 * <p>
 * 
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 * 
 */
public class MetaInformationCreator {

    /**
     * <p>
     * 
     * </p>
     */
    private final static Logger LOGGER = Logger.getLogger(MetaInformationCreator.class);

    /**
     * <p>
     * 
     * </p>
     */
    private final static Integer THREAD_POOL_SIZE = 100;

    /**
     * <p>
     * 
     * </p>
     */
    private FeedStore feedStore;

    public static int counter = 0;
    public static int collectionSize = 0;

    /**
     * <p>
     * 
     * </p>
     * 
     */
    public MetaInformationCreator() {
        feedStore = FeedDatabase.getInstance();
    }


    /**
     * <p>
     * 
     * </p>
     *
     */
    public void createMetaInformation() {
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        Collection<Feed> feedCollection = feedStore.getFeeds();

        LOGGER.info("start meta information gathering process");

        collectionSize = feedCollection.size();
        for (Feed feed : feedCollection) {
            MetaInformationCreationTask command = new MetaInformationCreationTask(feed);
            threadPool.execute(command);
        }
    }

    /**
     * <p>
     * 
     * </p>
     * 
     * @param args
     */
    public static void main(String[] args) {
        MetaInformationCreator creator = new MetaInformationCreator();
        creator.createMetaInformation();
    }
}
