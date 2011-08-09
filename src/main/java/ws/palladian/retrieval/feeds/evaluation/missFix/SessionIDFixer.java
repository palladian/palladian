package ws.palladian.retrieval.feeds.evaluation.missFix;

import java.util.Timer;

import org.apache.log4j.Logger;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;

public class SessionIDFixer {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(SessionIDFixer.class);

    /**
     * Schedule all {@link GZFeedTask}s
     */
    private Timer checkScheduler;

    /**
     * Defines the time in milliseconds when the checkScheduler should wake up to see if all feeds processed.
     */
    private final long wakeUpInterval = 60 * DateHelper.SECOND_MS;

    public SessionIDFixer() {
        checkScheduler = new Timer();
    }

    // /**
    // * Start removing false positive MISSes in the dataset.
    // */
    // public void removeFalseMisses() {
    //
    // final FeedDatabase feedStore = DatabaseManagerFactory.create(FeedDatabase.class);
    //
    // // FeedReader feedReader = new FeedReader(feedStore);
    //
    // FeedRetriever feedRetriever = new FeedRetriever();
    //
    // // FeedReaderEvaluator.setBenchmarkPolicy(FeedReaderEvaluator.BENCHMARK_OFF);
    //
    // // MAVStrategyDatasetCreation updateStrategy = new MAVStrategyDatasetCreation();
    // //
    // // updateStrategy.setHighestUpdateInterval(360); // 6hrs
    // // updateStrategy.setLowestUpdateInterval(0);
    // // feedReader.setUpdateStrategy(updateStrategy, true);
    //
    // // TODO: use a ProcessingAction here??
    // // FeedProcessingAction fpa = new DatasetProcessingAction(feedStore);
    // // feedChecker.setFeedProcessingAction(fpa);
    //
    // LOGGER.debug("start removing false positive MISSes.");
    // // feedReader.startContinuousReading();
    //
    // for (Feed dbFeed : feedStore.getFeeds()) {
    // String safeFeedName = DatasetCreator.getSafeFeedName(dbFeed.getFeedUrl());
    // String folderPath = DatasetCreator.getFolderPath(dbFeed.getId());
    // String csvPath = DatasetCreator.getCSVFilePath(dbFeed.getId(), safeFeedName);
    // File originalCsv = new File(csvPath);
    // String newCsvPath = FileHelper.getRenamedFilename(originalCsv, originalCsv.getName() + ".bak");
    // FileHelper.renameFile(originalCsv, newCsvPath);
    //
    // Feed correctedFeed = copyRequiredFeedProperties(dbFeed);
    // int oldChecks = dbFeed.getChecks();
    //
    // int filesProcessed = 0;
    // File[] allFiles = FileHelper.getFiles(folderPath, ".gz");
    // for (File file : allFiles) {
    //
    // // if (!file.getPath().endsWith(".gz")) {
    // // // LOGGER.debug("No gz file, continue: " + file.getName());
    // // continue;
    // // }
    // // // LOGGER.debug("Found file " + file.getName());
    //
    // HttpResult gzHttpResult = feedRetriever.loadSerializedGzip(file);
    // Feed gzFeed = null;
    // try {
    // gzFeed = feedRetriever.getFeed(gzHttpResult);
    // } catch (FeedRetrieverException e) {
    // LOGGER.error("Could not read feed from file " + file.getAbsolutePath() + " . "
    // + e.getLocalizedMessage());
    // continue;
    // }
    // correctedFeed.increaseChecks();
    // correctedFeed.setItems(gzFeed.getItems());
    // correctedFeed.setLastPollTime(getChecktimeFromFile(file));
    // correctedFeed.setLastSuccessfulCheckTime(correctedFeed.getLastPollTime());
    // correctedFeed.setWindowSize(gzFeed.getItems().size());
    //
    // SessionIDFixProcessingAction processingAction = new SessionIDFixProcessingAction(feedStore);
    // processingAction.performAction(correctedFeed, gzHttpResult);
    //
    // if (!correctedFeed.hasNewItem()) {
    // String newGzPath = FileHelper.getRenamedFilename(file, file.getName() + ".removeable");
    // FileHelper.renameFile(file, newGzPath);
    // }
    //
    // filesProcessed++;
    // if ((filesProcessed % 50) == 0) {
    // LOGGER.info("Processed " + filesProcessed + " so far.");
    // }
    // }
    // // write feed back to db, ignore meta information and item cache
    // feedStore.updateFeed(correctedFeed, false, false);
    //
    // if (correctedFeed.getChecks() != oldChecks) {
    // LOGGER.fatal("Less checks in corrected feed than in origial from feed-104!!");
    // }
    // }
    //
    // }
    //
    // /**
    // * Get the time this feed has been checked from the timestamp in millisecond in the file name. It is assumed
    // * that the file name starts with "<Java-Timestamp>_"
    // *
    // * @param file File to get timestamp from
    // * @return The timestamp.
    // */
    // private Date getChecktimeFromFile(File file) {
    // Date checkTime = null;
    // String fileName = file.getName();
    // Long timestamp = Long.parseLong(fileName.substring(0, fileName.indexOf("_")));
    // checkTime = new Date(timestamp);
    // return checkTime;
    // }

    // /**
    // * Creates a new feed that has most but not all of the properties the provided feed has.
    // *
    // * @param dbFeed feed to copy.
    // * @return New feed with partly copied properties.
    // */
    // private Feed copyRequiredFeedProperties(Feed dbFeed) {
    // Feed newFeed = new Feed();
    // newFeed.setActivityPattern(dbFeed.getActivityPattern());
    // newFeed.setAdditionalData(dbFeed.getAdditionalData());
    // newFeed.setBlocked(dbFeed.isBlocked());
    // // newFeed.setCachedItems(dbFeed.getCachedItems());
    // newFeed.setFeedMetaInformation(dbFeed.getMetaInformation());
    // newFeed.setFeedUrl(dbFeed.getFeedUrl());
    // newFeed.setHttpLastModified(dbFeed.getHttpLastModified());
    // newFeed.setId(dbFeed.getId());
    // newFeed.setLastFeedTaskResult(dbFeed.getLastFeedTaskResult());
    // newFeed.setTotalProcessingTime(dbFeed.getTotalProcessingTime());
    // newFeed.setUnparsableCount(dbFeed.getUnparsableCount());
    // newFeed.setUnreachableCount(dbFeed.getUnreachableCount());
    // newFeed.setUpdateInterval(dbFeed.getUpdateInterval());
    // return newFeed;
    // }

    
    public void removeFalseMisses() {

        final FeedDatabase feedStore = DatabaseManagerFactory.create(FeedDatabase.class);
        FeedReader feedChecker = new FeedReader(feedStore);

        SessionIDFixProcessingAction fpa = new SessionIDFixProcessingAction(feedStore);
        feedChecker.setFeedProcessingAction(fpa);

        GZScheduler gzScheduler = new GZScheduler(feedChecker);
        checkScheduler.schedule(gzScheduler, 0, wakeUpInterval);

    }

    /**
     * Run correction of false the feed dataset from all feeds in the database if possible.
     * 
     * @param args Command line arguments are ignored.
     */
    public static void main(String[] args) {
        // long time = 1310156530697L;
        // long ohneMS = (long) ((Math.ceil(time / 1000)) * 1000);
        // Timestamp test = new Timestamp(ohneMS);
        // System.out.println(test);
        // System.exit(0);

        SessionIDFixer sidFixer = new SessionIDFixer();
        sidFixer.removeFalseMisses();
    }

}
