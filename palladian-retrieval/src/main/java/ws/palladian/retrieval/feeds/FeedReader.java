package ws.palladian.retrieval.feeds;

import java.util.Timer;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.constants.SizeUnit;
import ws.palladian.retrieval.HttpRetriever;

/**
 * <p>
 * The FeedReader reads news from feeds in a database. It learns when it is necessary to check the feed again for news.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class FeedReader {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FeedReader.class);

    private final FeedReaderSettings settings;

    /**
     * A scheduler that checks continuously if there are feeds in the {@link #feedCollection} that need to be updated. A
     * feed must be updated whenever the method {@link Feed#getLastPollTime()} return value is further away in the past
     * then its {@link Feed#getMaxCheckInterval()} or {@link Feed#getUpdateInterval()} returns. Which one to use depends
     * on the update strategy.
     */
    private final Timer checkScheduler;

    public FeedReader(FeedReaderSettings settings) {
        Validate.notNull(settings, "settings must not be null");
        this.settings = settings;
        this.checkScheduler = new Timer();
    }

    /**
     * <p>
     * Start reading.
     * </p>
     */
    public void start() {
        SchedulerTask schedulerTask = new SchedulerTask(settings);
        checkScheduler.schedule(schedulerTask, 0, settings.getWakeUpInterval());

        LOGGER.debug(
                "Scheduled task, wake up every {} milliseconds to check all feeds whether they need to be read or not",
                settings.getWakeUpInterval());
    }

    /**
     * <p>
     * Stop reading.
     * </p>
     */
    public void stop() {
        checkScheduler.cancel();
        LOGGER.info("Cancelled all scheduled readings, total size downloaded ({}): {} MB",
                settings.getUpdateStrategy(), HttpRetriever.getTraffic(SizeUnit.MEGABYTES));
    }

}
