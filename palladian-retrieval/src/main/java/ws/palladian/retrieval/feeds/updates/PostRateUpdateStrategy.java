package ws.palladian.retrieval.feeds.updates;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedPostStatistics;
import ws.palladian.retrieval.feeds.evaluation.FeedReaderEvaluator;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

/**
 * <p>
 * Predict the next item post time by looking at the feed item post distribution and remembering it.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public class PostRateUpdateStrategy extends AbstractUpdateStrategy {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(MavUpdateStrategy.class);
    
    private final FeedUpdateMode updateMode;

    public PostRateUpdateStrategy(int lowestInterval, int highestInterval, FeedUpdateMode updateMode) {
        super(lowestInterval, highestInterval);
        this.updateMode = updateMode;
    }

    /**
     * <p>
     * Update the update interval for the feed given the post statistics.
     * </p>
     * 
     * @param feed The feed to update.
     * @param fps This feeds feed post statistics.
     * @param trainingMode Ignored parameter. The strategy does not support an explicit training mode.
     */
    @Override
    public void update(Feed feed, FeedPostStatistics fps, boolean trainingMode) {

        if (trainingMode) {
            LOGGER.warn("Update strategy " + getName() + " does not support an explicit training mode.");
        }
        List<FeedItem> entries = feed.getItems();

        // learn the post distribution from the last seen entry to the newest one
        // distribution minute of the day : frequency of news in that minute
        Map<Integer, int[]> postDistribution = null;

        if (feed.getChecks() == 0) {
            postDistribution = new HashMap<Integer, int[]>();

            // since the feed has no post distribution yet, we fill all minutes with 0 posts
            for (int minute = 0; minute < 1440; minute++) {
                int[] postsChances = { 0, 0 };
                postDistribution.put(minute, postsChances);
            }

        } else {
            postDistribution = feed.getMeticulousPostDistribution();

            // in benchmark mode we keep it in memory
            if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
                // TODO database should be injected.
                FeedDatabase fd = DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig());
                postDistribution = fd.getFeedPostDistribution(feed);
            }

        }

        // update the minutes where an entry could have been posted
        int minuteCounter = 0;
        long timeLastSeenEntry = Long.MIN_VALUE;
        if (feed.getLastFeedEntry() != null) {
            timeLastSeenEntry = feed.getLastFeedEntry().getTime();
        }
        int startMinute = (int) DateHelper.getTimeOfDay(fps.getTimeOldestPost(), Calendar.MINUTE);
        for (long t = fps.getTimeOldestPost(); t < fps.getTimeNewestPost() + TimeUnit.MINUTES.toMillis(1); t += TimeUnit.MINUTES.toMillis(1), minuteCounter++) {
            // we have counted the chances for entries before the last seen entry already, so we skip them here
            if (t <= timeLastSeenEntry) {
                continue;
            }
            int minuteOfDay = (startMinute + minuteCounter) % 1440;
            int[] postsChances = postDistribution.get(minuteOfDay);
            postsChances[1] = postsChances[1] + 1;
            postDistribution.put(minuteOfDay, postsChances);
        }

        // update the minutes where an entry was actually posted
        for (FeedItem entry : entries) {
            // we have counted the posts for entries before the last seen entry already, so we skip them here
            if (entry.getPublished() == null || entry.getPublished().getTime() <= timeLastSeenEntry) {
                continue;
            }
            int minuteOfDay = (int) DateHelper.getTimeOfDay(entry.getPublished(), Calendar.MINUTE);
            int[] postsChances = postDistribution.get(minuteOfDay);
            postsChances[0] = postsChances[0] + 1;
            postDistribution.put(minuteOfDay, postsChances);
        }

        feed.setMeticulousPostDistribution(postDistribution);

        // in benchmark mode we keep it in memory, in real usage, we store the distribution in the database
        if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
            // TODO database should be injected.
            FeedDatabase fd = DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig());
            fd.updateFeedPostDistribution(feed, postDistribution);
        }

        // only use calculated update intervals if one full day of distribution is available already

        startMinute = 0;

        if (FeedReaderEvaluator.getBenchmarkPolicy() == FeedReaderEvaluator.BENCHMARK_OFF) {
            startMinute = (int) DateHelper.getTimeOfDay(System.currentTimeMillis(), Calendar.MINUTE);
        } else {
            startMinute = (int) DateHelper.getTimeOfDay(feed.getBenchmarkLookupTime(), Calendar.MINUTE);
        }

        // // estimate time to next entry and time until list is full with
        // only new but one entries

        // set to one month maximum
        int minCheckInterval = 31 * 1440;
        boolean minCheckIntervalFound = false;

        // set to six month maximum
        int maxCheckInterval = 6 * 31 * 1440;

        // add up all probabilities for the coming minutes until the
        // estimated post number is 1
        int currentMinute = startMinute;
        double estimatedPosts = 0;
        for (int c = 0; c < maxCheckInterval; c++) {

            int[] postsChances = postDistribution.get(currentMinute);
            double postProbability = 0;
            if (postsChances[1] > 0) {
                postProbability = (double) postsChances[0] / (double) postsChances[1];
            }
            estimatedPosts += postProbability;

            if (estimatedPosts >= 1 && !minCheckIntervalFound) {
                minCheckInterval = c;
                minCheckIntervalFound = true;
            }

            if (estimatedPosts >= entries.size()) {
                maxCheckInterval = c;
                break;
            }

            currentMinute = (currentMinute + 1) % 1440;
        }

        if (updateMode == FeedUpdateMode.MIN_DELAY) {
            feed.setUpdateInterval(getAllowedInterval(minCheckInterval));
        } else {
            feed.setUpdateInterval(getAllowedInterval(maxCheckInterval));
        }
    }

    @Override
    public String getName() {
        return "postrate";
    }

    @Override
    public boolean hasExplicitTrainingMode() {
        return false;
    }
}
