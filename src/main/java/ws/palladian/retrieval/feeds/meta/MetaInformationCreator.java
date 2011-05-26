/**
 * Created on: 28.07.2010 17:43:02
 */
package ws.palladian.retrieval.feeds.meta;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.persistence.RowConverter;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

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
 * @author Philipp Katz
 * @version 1.0
 * @since 1.0
 * 
 */
public class MetaInformationCreator {

    private final static Logger LOGGER = Logger.getLogger(MetaInformationCreator.class);

    /** Maximum number of feed metadata reading threads at the same time. */
    public static final Integer DEFAULT_THREAD_POOL_SIZE = 200;

    private static Integer THREAD_POOL_SIZE = DEFAULT_THREAD_POOL_SIZE;

    private final FeedDatabase feedDatabase;

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
        feedDatabase = (FeedDatabase) DatabaseManagerFactory.getInstance().create(FeedDatabase.class.getName());
        this.feedIdentifierLowerBound = feedDatabase.runSingleQuery(new IntegerRowConverter(),
                "SELECT MIN(id) FROM feeds");
        this.feedIdentifierUpperBound = feedDatabase.runSingleQuery(new IntegerRowConverter(),
                "SELECT MAX(id) FROM feeds");
        this.availableFeedIdentifier = feedDatabase.runQuery(new IntegerRowConverter(), "SELECT id from feeds");
        loadConfig();
    }

    /**
     * @param string
     * @param string2
     */
    public MetaInformationCreator(Integer feedIdentifierLowerBound, Integer feedIdentifierUpperBound) {
        feedDatabase = (FeedDatabase) DatabaseManagerFactory.getInstance().create(FeedDatabase.class.getName());
        this.availableFeedIdentifier = feedDatabase.runQuery(new IntegerRowConverter(), "SELECT id from feeds");
        this.feedIdentifierLowerBound = feedIdentifierLowerBound;
        this.feedIdentifierUpperBound = feedIdentifierUpperBound;
        loadConfig();

    }

    public void createMetaInformation() {
        collectionSize = feedIdentifierUpperBound - feedIdentifierLowerBound;
        ExecutorService threadPool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        LOGGER.info("start meta information gathering process");

        for (Integer feedIdentifier : availableFeedIdentifier) {
            if (feedIdentifier >= feedIdentifierLowerBound && feedIdentifier <= feedIdentifierUpperBound) {
                Feed feed = feedDatabase.getFeedByID(feedIdentifier);
                MetaInformationCreationTask command = new MetaInformationCreationTask(feed, feedDatabase);
                threadPool.execute(command);
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    /**
     * Load thread pool size from palladian.properties
     */
    private void loadConfig() {
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        if (config != null) {
            THREAD_POOL_SIZE = config.getInteger("metaInformationCreator.threadPoolSize", DEFAULT_THREAD_POOL_SIZE);
        } else {
            LOGGER.warn("could not load configuration, use defaults");
        }
    }
}
