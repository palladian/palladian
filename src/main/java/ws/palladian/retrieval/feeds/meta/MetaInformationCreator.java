/**
 * Created on: 28.07.2010 17:43:02
 */
package ws.palladian.retrieval.feeds.meta;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.Logger;

import ws.palladian.persistence.DatabaseManager;
import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

class IntegerRowConverter implements RowConverter<Integer> {

    @Override
    public Integer convert(ResultSet resultSet) throws SQLException {
        return resultSet.getInt(1);
    }

}

/**
 * <p>
 * Creates meta information about the capabilities of some feeds.
 * </p>
 * 
 * @author Klemens Muthmann
 * @version 1.0
 * @since 1.0
 * 
 */
public class MetaInformationCreator {

    private final static Logger LOGGER = Logger.getLogger(MetaInformationCreator.class);

    private final static Integer THREAD_POOL_SIZE = 100;

    private final FeedStore feedStore;

    private final DatabaseManager dbManager;

    private final Integer feedIdentifierLowerBound;
    private final Integer feedIdentifierUpperBound;
    private final Collection<Integer> availableFeedIdentifier;

    public static int counter = 0;
    public static int collectionSize = 0;

    public static void main(String[] args) {
        MetaInformationCreator creator;
        if (args.length == 2) {
            creator = new MetaInformationCreator(Integer.valueOf(args[0]), Integer.valueOf(args[1]));
        } else {
            creator = new MetaInformationCreator();
        }
        creator.createMetaInformation();
    }

    public MetaInformationCreator() {
        dbManager = new DatabaseManager();
        feedStore = new FeedDatabase();
        this.feedIdentifierLowerBound = dbManager
                .runSingleQuery(new IntegerRowConverter(), "SELECT MIN(id) FROM feeds");
        this.feedIdentifierUpperBound = dbManager
                .runSingleQuery(new IntegerRowConverter(), "SELECT MAX(id) FROM feeds");
        this.availableFeedIdentifier = dbManager.runQuery(new IntegerRowConverter(), "SELECT id from feeds");
    }

    /**
     * @param string
     * @param string2
     */
    public MetaInformationCreator(Integer feedIdentifierLowerBound, Integer feedIdentifierUpperBound) {
        dbManager = new DatabaseManager();
        feedStore = new FeedDatabase();
        this.availableFeedIdentifier = dbManager.runQuery(new IntegerRowConverter(), "SELECT id from feeds");
        this.feedIdentifierLowerBound = feedIdentifierLowerBound;
        this.feedIdentifierUpperBound = feedIdentifierUpperBound;

    }

    public void createMetaInformation() {
        collectionSize = feedIdentifierUpperBound - feedIdentifierLowerBound;
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        LOGGER.info("start meta information gathering process");

        for (Integer feedIdentifier : availableFeedIdentifier) {
            if (feedIdentifier >= feedIdentifierLowerBound && feedIdentifier <= feedIdentifierUpperBound) {
                Feed feed = feedStore.getFeedByID(feedIdentifier);
                MetaInformationCreationTask command = new MetaInformationCreationTask(feed, dbManager);
                threadPool.execute(command);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }
}
