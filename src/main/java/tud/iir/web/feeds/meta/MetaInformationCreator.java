/**
 * Created on: 28.07.2010 17:43:02
 */
package tud.iir.web.feeds.meta;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import tud.iir.web.feeds.Feed;
import tud.iir.web.feeds.FeedDatabase;
import tud.iir.web.feeds.FeedStore;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 * 
 */
public class MetaInformationCreator {

    private final static Logger LOGGER = Logger.getLogger(MetaInformationCreator.class);

    private final static Integer THREAD_POOL_SIZE = 100;

    private FeedStore feedStore;

    public static int counter = 0;
    public static int collectionSize = 0;


    public MetaInformationCreator() {
        feedStore = FeedDatabase.getInstance();
    }


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


    public static void main(String[] args) {
        MetaInformationCreator creator = new MetaInformationCreator();
        creator.createMetaInformation();
    }
}
