//package ws.palladian.retrieval.feeds.evaluation.gzPorcessing;
//
//import java.io.BufferedReader;
//import java.io.FileInputStream;
//import java.io.FileNotFoundException;
//import java.io.IOException;
//import java.io.InputStreamReader;
//import java.io.UnsupportedEncodingException;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.List;
//import java.util.Map;
//
//import org.apache.log4j.Logger;
//
//import ws.palladian.helper.ConfigHolder;
//import ws.palladian.helper.io.FileHelper;
//import ws.palladian.persistence.DatabaseManagerFactory;
//import ws.palladian.retrieval.feeds.Feed;
//import ws.palladian.retrieval.feeds.FeedItem;
//import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;
//import ws.palladian.retrieval.feeds.meta.PollMetaInformation;
//import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
//
///**
// * <p>Quick'n'dirty worker thread to read all items from csv, read httpDate from db table feed_polls and classify a feed based on ALL
// * items.</p>
// * 
// * <p>Required for iiWAS feed dataset paper using TUDCS6 dataset.</p>
// * 
// * @author Sandro Reichert
// * 
// */
//public class ClassifyFromCsv extends Thread {
//
//    /** The logger for this class. */
//    private static final Logger LOGGER = Logger.getLogger(ClassifyFromCsv.class);
//
//    private Feed originalFeed;
//
//    private Feed tempFeed = new Feed();
//
//    public static final String BACKUP_FILE_EXTENSION = ".6rows";
//
//    private FeedDatabase feedStore = DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig());
//
//    // Map(pollTimestamp -> httpDate)
//    private Map<Long, Date> pollMap = new HashMap<Long, Date>();
//
//    /**
//     * @param feed the Feed to process
//     */
//    public ClassifyFromCsv(Feed feed) {
//        this.originalFeed = feed;
//    }
//
//    @Override
//    public void run() {
//        try {
//            // get path to csv
//            // String safeFeedName = StringHelper.makeSafeName(feed.getFeedUrl().replaceFirst("http://www.", "")
//            // .replaceFirst("www.", ""), 30);
//            //
//            // int slice = (int) Math.floor(feed.getId() / 1000.0);
//            //
//            // String folderPath = DatasetCreator.DATASET_PATH + slice + "/" + feed.getId() + "/";
//            // String csvPath = folderPath + feed.getId() + "_" + safeFeedName + ".csv";
//            String csvPath = DatasetCreator.getCSVFilePath(originalFeed.getId(),
//                    DatasetCreator.getSafeFeedName(originalFeed.getFeedUrl()));
//
//            // read csv
//            LOGGER.debug("processing: " + csvPath);
//            if (!FileHelper.fileExists(csvPath)) {
//                LOGGER.fatal("No csv file found for feed id " + originalFeed.getId() + ", tried to get file " + csvPath
//                        + ". Nothing to do for this feed.");
//                return;
//            }
//
//            getFeedPollsFromDB();
//            readItemsFromCSV(csvPath);
//
//            originalFeed.setActivityPattern(ExperimentalFeedClassifier.classify(tempFeed));
//            boolean success = feedStore.updateFeed(originalFeed);
//
//            if (!success) {
//                LOGGER.fatal("Feed id " + originalFeed.getId()
//                        + " could not be written to database. New activity pattern is "
//                        + originalFeed.getActivityPattern());
//            }
//
//            // if (backupOriginal && newFileWritten) {
//            // LOGGER.info("New file written to " + csvPath);
//            // } else {
//            // LOGGER.fatal("could not write output file, dumping to log:\n" + rewrittenCSV);
//            // }
//
//            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
//            // killed by the thread pool internals.
//        } catch (Throwable th) {
//            LOGGER.error(th);
//        }
//
//    }
//
//    /**
//     * Reads the csv file at the specified path, get timestamps for each item, build item and add to
//     * {@link #originalFeed}.
//     * 
//     * @param csvPath Path to csv file to read.
//     */
//    private void readItemsFromCSV(String csvPath) {
//
//        // FileHelper does not set an encoding explicitly, this causes trouble, when our test cases
//        // are run via maven. I suppose the problem has to do with a different default encoding,
//        // although everything seems to be configured correctly at first glance. We should think whether
//        // it makes sense to always explicitly set UTF-8 when reading files. -- Philipp.
//        BufferedReader reader = null;
//        try {
//            reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), "UTF-8"));
//            String line = null;
//            int lineCounter = 0;
//            while ((line = reader.readLine()) != null) {
//                lineCounter++;
//                String[] split = line.split(";");
//
//                // ignore MISS line
//                if (!split[0].startsWith("MISS")) {
//
//                    try {
//                        // get timestamps and windowSize from csv
//                        String csvPublishDate = split[0];
//                        Date publishDate = new Date(Long.parseLong(csvPublishDate));
//                        Date pollTime = new Date(Long.parseLong(split[1]));
//                        String hash = split[2];
//                        int windowSize = Integer.parseInt(split[5]);
//
//                        // get corrected publish date
//                        String logMessage = "Feed id " + originalFeed.getId() + " line " + lineCounter;
//                        Date correctedPublishDate = Feed.correctedTimestamp(publishDate, pollTime, null, logMessage, false);
//
//                        // set pollTime if entry has no publish time. In case the item had no publish date, we wrote
//                        // 0000000000000 to the csv file to indicate a not existing publish date
//                        if (csvPublishDate.equals("0000000000000")) {
//                            correctedPublishDate = pollTime;
//                            publishDate = null;
//                        }
//
//                        // tricky: we have timestamps with millisecond precision in csv but only second in database.
//                        long pollTimeWithoutMillisecond = (long) ((Math.ceil(pollTime.getTime() / 1000)) * 1000);
//
//                        // create new, minimal item and add to feed
//                        FeedItem item = new FeedItem();
//                        item.setFeed(tempFeed);
//                        item.setHash(hash, true);
//                        item.setPublished(publishDate);
//                        item.setCorrectedPublishedDate(correctedPublishDate);
//                        item.setHttpDate(pollMap.get(pollTimeWithoutMillisecond));
//                        item.setWindowSize(windowSize);
//                        tempFeed.addItem(item);
//                    } catch (NumberFormatException e) {
//                        LOGGER.fatal("Could not get number from csv: " + e.getLocalizedMessage());
//                    }
//                }
//            }
//        } catch (FileNotFoundException e) {
//            LOGGER.error(e);
//        } catch (UnsupportedEncodingException e) {
//            LOGGER.error(e);
//        } catch (IOException e) {
//            LOGGER.error(e);
//        } finally {
//            FileHelper.close(reader);
//        }
//    }
//
//    /**
//     * Get {@link PollMetaInformation} for each poll that has been made on this feed and fills {@link #pollMap}.
//     * Additionally, {@link Feed#setLastPollTime(Date)} and {@link Feed#setHttpDateLastPoll(Date)} are set
//     * 
//     */
//    private void getFeedPollsFromDB() {
//        List<PollMetaInformation> pollMetaInfos = feedStore.getFeedPollsByID(originalFeed.getId());
//        Date lastPollTime = null;
//        Date lastHttpDate = null;
//
//        for (PollMetaInformation poll : pollMetaInfos) {
//            Date pollTime = poll.getPollTimestamp();
//            Date httpDate = poll.getHttpDate();
//            if (pollTime != null) {
//                if (lastPollTime == null || pollTime.getTime() > lastPollTime.getTime()) {
//                    lastPollTime = pollTime;
//                }
//
//                if (httpDate != null) {
//                    pollMap.put(pollTime.getTime(), httpDate);
//
//                    if (lastHttpDate == null || httpDate.getTime() > lastHttpDate.getTime()) {
//                        lastHttpDate = httpDate;
//                    }
//                }
//            }
//        }
//        tempFeed.setLastPollTime(lastPollTime);
//        tempFeed.setHttpDateLastPoll(lastHttpDate);
//    }
//
//    // /**
//    // * @param args
//    // * @throws FeedParserException
//    // */
//    // public static void main(String[] args) throws FeedParserException {
//    //
//    // RomeFeedParser feedRetriever = new RomeFeedParser();
//    // File file = new File(
//    // "F:\\Workspaces\\Eclipse\\Palladian\\trunk\\data\\datasets\\feedPosts\\4\\4271\\1311030075575_2011-07-19_01-01-15.gz");
//    // Feed gzFeed = feedRetriever.getFeed(file, true);
//    // System.out.println(gzFeed);
//    //
//    //
//    // }
//
//}
