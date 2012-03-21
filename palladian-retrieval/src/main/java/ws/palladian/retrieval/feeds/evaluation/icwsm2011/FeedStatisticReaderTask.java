package ws.palladian.retrieval.feeds.evaluation.icwsm2011;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

/**
 * <p>Reads the feed's csv, extracts the windowSize, checks whether it is variable or static, gets number of items and
 * number of misses and stores these values in the database.</p>
 * 
 * <p>Class might be used after merging multiple csv files to get these statistics which are usually directly written to
 * database when crawling the feed.</p>
 * 
 * @author Sandro Reichert
 * 
 */
public class FeedStatisticReaderTask extends Thread {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(FeedStatisticReaderTask.class);

    /**
     * The feed checker calling this task. // FIXME This is a workaround. Can be fixed by externalizing update
     * strategies to a true strategy pattern.
     */
    private final FeedStore feedStore;

    /** The {@link Feed} to merge. */
    private final Feed feed;

    /** The position of the window size in a column in the csv, starting with 0! */
    private static final int WINDOW_SIZE_POSIION = 5;

    /**
     * @param feed The {@link Feed} to merge.
     */
    public FeedStatisticReaderTask(FeedStore feedStore, Feed feed) {
        this.feedStore = feedStore;
        this.feed = feed;
    }

    @Override
    public void run() {
        try {
            LOGGER.debug("processing feed " + feed.getId());
            String dataPath = getRelativePathToFeed();
            String csvFileName = getCSVFileName();

            // read csv files
            if (!FileHelper.fileExists(dataPath + csvFileName)) {
                LOGGER.fatal("No csv base file found for feed id " + feed.getId() + ", tried to read file " + dataPath
                        + csvFileName + ". Nothing to do for this feed.");
                return;
            }
            if (!FileHelper.fileExists(dataPath + csvFileName)) {
                LOGGER.fatal("No csv file to merge into base found for feed id " + feed.getId()
                        + ", tried to read file " + dataPath + csvFileName + ". Nothing to do for this feed.");
                return;
            }
            // get items from file
            List<String> items = readCsv(dataPath + csvFileName);

            int windowSize = 0;
            int itemCounter = 0;
            int missCounter = 0;

            for (int currentLineNr = 0; currentLineNr < items.size(); currentLineNr++) {

                String[] currentLine = items.get(currentLineNr).split(";");

                if (currentLine[WINDOW_SIZE_POSIION].equals("MISS")) {
                    missCounter++;
                    continue;
                }

                itemCounter++;
                int currentWindowSize = Integer.parseInt(currentLine[WINDOW_SIZE_POSIION]);

                if (windowSize != 0 && windowSize != currentWindowSize) {
                    feed.setVariableWindowSize(true);
                    break;
                } else {
                    windowSize = currentWindowSize;
                }
            }

            feed.setWindowSize(windowSize);
            feed.setNumberOfItemsReceived(itemCounter);
            feed.setMisses(missCounter);

            // save the feed back to the database
            feedStore.updateFeed(feed);

            LOGGER.info("Feed id: " + feed.getId() + ", window size: " + windowSize + ", items: " + itemCounter
                    + ", misses: " + missCounter);

            // This is ugly but required to catch everything. If we skip this, threads may run much longer till they are
            // killed by the thread pool internals.
        } catch (Throwable th) {
            LOGGER.error(th);
        }
    }

    /* package */static List<String> readCsv(String csvPath) {
        // List<String> items = FileHelper.readFileToArray(csvPath);
        // return items;

        // FileHelper does not set an encoding explicitly, this causes trouble, when our test cases
        // are run via maven. I suppose the problem has to do with a different default encoding,
        // although everything seems to be configured correctly at first glance. We should think whether
        // it makes sense to always explicitly set UTF-8 when reading files. -- Philipp.
        BufferedReader reader = null;
        List<String> result = new ArrayList<String>();
        try {
            reader = new BufferedReader(new InputStreamReader(new FileInputStream(csvPath), "UTF-8"));
            String line = null;
            while ((line = reader.readLine()) != null) {
                result.add(line);
            }
        } catch (FileNotFoundException e) {
            LOGGER.error(e);
        } catch (UnsupportedEncodingException e) {
            LOGGER.error(e);
        } catch (IOException e) {
            LOGGER.error(e);
        } finally {
            FileHelper.close(reader);
        }
        return result;

    }

    /**
     * Relative path to feed data, such as "data/datasets/feedPosts/0/100/" for feed id 100.
     * 
     * @return relative path to feed data.
     */
    private String getRelativePathToFeed() {

        int slice = (int) Math.floor(feed.getId() / 1000.0);
        String folderPath = DatasetCreator.DATASET_PATH + slice + "/" + feed.getId() + "/";
        LOGGER.debug("Relative Path to feed: " + folderPath);
        return folderPath;
    }

    /**
     * @return The name of the csv file.
     */
    private String getCSVFileName() {
        String safeFeedName = StringHelper.makeSafeName(feed.getFeedUrl().replaceFirst("http://www.", "").replaceFirst(
                "www.", ""), 30);
        String fileName = feed.getId() + "_" + safeFeedName + ".csv";
        LOGGER.debug("CSV filename: " + fileName);
        return fileName;
    }

    public static void main(String[] args) {

        FeedStore feedStore = DatabaseManagerFactory.create(FeedDatabase.class, ConfigHolder.getInstance().getConfig());
        Feed feed = new Feed();
        feed.getMetaInformation().setSiteUrl("");

        // test case with variable window size
        // feed.setId(1001);
        // feed.setFeedUrl("http://511virginia.org/rss/Northern/Events.ashx");

        // test case with window size 10
        feed.setId(1);
        feed.setFeedUrl("http://007fanart.wordpress.com/comments/feed/");

        // test case with variable window size and large csv
        feed.setId(174749);
        feed.setFeedUrl("http://www.panoramio.com/userfeed/");

        FeedStatisticReaderTask chart = new FeedStatisticReaderTask(feedStore, feed);
        chart.run();
    }

}
