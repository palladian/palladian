/**
 * Created on: 28.07.2010 17:43:02
 */
package ws.palladian.retrieval.feeds.meta;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * <p>
 * Creates meta information about the capabilities of some feeds.
 * </p>
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
        feedStore = new FeedDatabase();
    }


    public void createMetaInformation() {
        ExecutorService threadPool = Executors.newCachedThreadPool();
        Collection<Feed> feedCollection = feedStore.getFeeds();

        LOGGER.info("start meta information gathering process");

        collectionSize = feedCollection.size();
        for (Feed feed : feedCollection) {
            MetaInformationCreationTask command = new MetaInformationCreationTask(feed);
            threadPool.execute(command);
            try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
        }
    }


    public static void main(String[] args) {
        MetaInformationCreator creator = new MetaInformationCreator();
        creator.createMetaInformation();
    }
}
