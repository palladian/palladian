package ws.palladian.retrieval.feeds.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.FileHelper;
import ws.palladian.helper.StopWatch;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.math.MathHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.persistence.DatabaseManagerFactory;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedProcessingAction;
import ws.palladian.retrieval.feeds.FeedReader;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.feeds.updates.MAVStrategyDatasetCreation;

/**
 * <p>
 * Creates a dataset of feed posts.
 * </p>
 * <p>
 * For each feed, a csv file is created in the data/datasets/feedPosts/ folder. Each file contains all distinct posts
 * collected over a period of time. Each file follows the follwowing layout:<br>
 * 
 * <pre>
 * ITEM_TIMESTAMP;POLL_TIMESTAMP;HASH;"TITLE";"LINK";WINDOWSIZE;
 * </pre>
 * <p>
 * If the creator finds a completely new window it must assume that it missed some entries and adds a line containing
 * <tt>MISS;;;;;;</tt>.
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * @author Sandro Reichert
 * 
 */
public class DatasetCreator {


    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetCreator.class);

    /** Path to the folder where the dataset is stored. */
    public static final String DATASET_PATH = "data" + System.getProperty("file.separator") + "datasets"
            + System.getProperty("file.separator") + "feedPosts" + System.getProperty("file.separator");

    /** We need this many file handles to process one FeedTask. */
    public static final int FILE_HANDLES_PER_TASK = 20;

    public static final boolean CHECK_SYSTEM_LIMITATIONS_DEFAULT = true;

    /** Used if item has no time stamp to write as default value. */
    public static final String NO_TIMESTAMP = "0000000000000";

    public DatasetCreator() {
        detectSystemLimitations();
    }



    /**
     * Cleaning up performs the following steps:
     * <ul>
     * <li>Remove all empty files from dataset folder.</li>
     * <li>Remove files which have posts with pub dates later than the current date.</li>
     * <li>Remove duplicate entries (key is title and link).</li>
     * <li>Remove MISS lines.</li>
     * <li>Sort entries by pubdate.</li>
     * </ul>
     */
    public static void cleanUp(boolean removeMISS) {

        StopWatch sw = new StopWatch();

        String cleanPath = DATASET_PATH + "clean\\";

        File[] files = FileHelper.getFiles(DATASET_PATH);
        int fileCount = files.length;
        int deleteCount = 0;
        int c = 0;
        for (File file : files) {
            c++;

            // if (!file.getName().startsWith("105989_")) {
            // continue;
            // }

            // remove empty files or files bigger than 25MB
            if (file.length() < 20 || file.length() > 26214400) {
                // file.delete();
                // deleteCount++;
                continue;
            }

            // skip directories
            else if (file.isDirectory()) {
                continue;
            }

            // check file
            else {

                String raw = FileHelper.readFileToString(file);
                // String cleansed = raw.replaceAll("(\t)+", "").replaceAll("\"(\n)+", "\"").replaceAll("(\n)+\"", "\"")
                // .replaceAll("(\n)(?=.)", "");

                String cleansed = raw.replaceAll("(\t)+", "").replaceAll("\"(\n)+", "\"").replaceAll("(\n)+\"", "\"")
                        .replaceAll("(\n)(?!((.*?\\d;\")|(.*?MISS;)))", "")
                        .replaceAll("(?<=\"http([^\"]){0,200});(?=(.)+\")", ":");

                FileHelper.writeToFile(cleanPath + file.getName(), cleansed);

                // to remove duplicates we save the hashes of the keys
                Set<Integer> hashCodes = new HashSet<Integer>();

                Set<String> sortedEntries = new TreeSet<String>();
                List<String> entries = FileHelper.readFileToArray(cleanPath + file.getName());

                for (String entry : entries) {

                    // remove MISS lines
                    if (entry.startsWith("MISS")) {
                        if (!removeMISS) {
                            sortedEntries.add(entry);
                        }
                        continue;
                    }

                    // remove feeds with window size of one
                    if (entry.endsWith(";1")) {
                        continue;
                    }

                    if (entry.indexOf(";\"") == -1 || entry.lastIndexOf("\";") == -1) {
                        LOGGER.warn("bad format in file " + file.getName() + " skip cleaning this entry");
                        continue;
                    }

                    // check whether timestamp is valid, that is, not newer than current timestamp or smaller than
                    // 946684800 (01/01/2000)
                    long timestamp = Long.valueOf(entry.substring(0, entry.indexOf(";")));
                    if (timestamp > System.currentTimeMillis() || timestamp < 946684800000l) {
                        LOGGER.info("timestamp " + timestamp + " is invalid, skip cleaning this entry");
                        continue;
                    } else if (timestamp < 1000000000000l) {
                        entry = entry.replaceFirst(String.valueOf(timestamp), "0" + timestamp);
                    }

                    String key = entry.substring(entry.indexOf(";\"") + 2, entry.lastIndexOf("\";"));

                    if (hashCodes.add(key.hashCode())) {
                        // sort
                        sortedEntries.add(entry);
                    }

                }

                StringBuilder sortedData = new StringBuilder();

                for (String entry : sortedEntries) {
                    sortedData.insert(0, entry + "\n");
                }
                FileHelper.writeToFile(cleanPath + file.getName(), sortedData);

                // System.out.println("cleansed " + file.getName());
                if (c % 500 == 0) {
                    LOGGER.info(MathHelper.round((double) 100 * c / fileCount, 2) + "% of the files cleansed");
                }

            }
        }

        // remove empty files again because there might be empty clean files now
        files = FileHelper.getFiles(cleanPath);
        for (File file : files) {

            if (file.length() == 0) {
                boolean deleted = file.delete();
                if (deleted) {
                    deleteCount++;
                }
            }
        }

        LOGGER.info("finished in " + sw.getElapsedTimeString() + ", deleted " + deleteCount + " files");
    }

    /**
     * We combine all feed histories into one file which we can then import into a database to generate statistics.
     */
    public static void combineFeedHistories() {

        StopWatch sw = new StopWatch();

        String cleanPath = DATASET_PATH + "clean\\";

        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter(cleanPath + "all.csv");
        } catch (IOException e1) {
            LOGGER.error(e1.getMessage());
        }

        int c = 0;
        File[] files = FileHelper.getFiles(cleanPath);
        for (File file : files) {

            String feedID = file.getName().substring(0, file.getName().indexOf("_"));

            List<String> contents = FileHelper.readFileToArray(file);

            for (String line : contents) {

                try {
                    fileWriter.write(feedID + ";");
                    fileWriter.write(line);
                    fileWriter.flush();
                } catch (IOException e) {
                    LOGGER.error(file + ", " + e.getMessage());
                }

            }

            c++;
            LOGGER.info("percent done: " + MathHelper.round(100 * c / (double) files.length, 2));
        }

        try {
            fileWriter.close();
        } catch (Exception e) {
            LOGGER.error(e.getMessage());
        }

        LOGGER.info("all files combined to all.csv in " + sw.getElapsedTimeString());
    }

    public static void renewFileIDs() {

        String cleanPath = "data/temp/feedPosts/";
        File[] files = FileHelper.getFiles(cleanPath);
        for (File file : files) {

            // skip directories
            if (file.isDirectory()) {
                continue;
            }

            int feedID = Integer.valueOf(file.getName().substring(0, file.getName().indexOf("_"))) + 97650;

            String fileNameRealID = feedID + file.getName().substring(file.getName().indexOf("_"));
            LOGGER.info(fileNameRealID);
            // FileHelper.rename(file, fileNameRealID);

            FileHelper.copyFile(cleanPath + file.getName(), cleanPath + fileNameRealID);
            FileHelper.delete(cleanPath + file.getName());

        }

    }

    /**
     * Start creating the dataset.
     */
    public void createDataset() {

        FeedStore feedStore = DatabaseManagerFactory.create(FeedDatabase.class);

        // all feeds need to be classified in advance to filter them accordingly
        // FeedClassifier.classifyFeedInStore(feedStore);

        FeedReader feedChecker = new FeedReader(feedStore);

        FeedReaderEvaluator.setBenchmarkPolicy(FeedReaderEvaluator.BENCHMARK_OFF);

        MAVStrategyDatasetCreation updateStrategy = new MAVStrategyDatasetCreation();

        updateStrategy.setHighestUpdateInterval(360); // 6hrs
        updateStrategy.setLowestUpdateInterval(5);
        feedChecker.setUpdateStrategy(updateStrategy, true);

        // create the dataset only with feeds that are parsable, have at least one entry, and are alive
        // Collection<Integer> updateClasses = new HashSet<Integer>();
        // updateClasses.add(FeedClassifier.CLASS_ZOMBIE);
        // updateClasses.add(FeedClassifier.CLASS_SPONTANEOUS);
        // updateClasses.add(FeedClassifier.CLASS_SLICED);
        // updateClasses.add(FeedClassifier.CLASS_SINGLE_ENTRY);
        // updateClasses.add(FeedClassifier.CLASS_ON_THE_FLY);
        // updateClasses.add(FeedClassifier.CLASS_CONSTANT);
        // updateClasses.add(FeedClassifier.CLASS_CHUNKED);
        // feedChecker.filterFeeds(updateClasses);

        FeedProcessingAction fpa = new FeedProcessingAction() {

            @Override
            public boolean performAction(Feed feed, HttpResult httpResult) {

                boolean success = true;

                // get all posts in the feed as timestamp;headline;link
                List<FeedItem> feedEntries = feed.getItems();

                if (feedEntries == null) {
                    LOGGER.warn("no feed entries for " + feed.getFeedUrl());
                    return success;
                }

                // get the path of the feed's folder and csv file
                String folderPath = getFolderPath(feed.getId());
                String filePath = getCSVFilePath(feed.getId(), getSafeFeedName(feed.getFeedUrl()));
                LOGGER.debug("saving feed to: " + filePath);

                success = DatasetCreator.createDirectoriesAndCSV(feed);

                // load only the last window from file
                int recentWindowSize = feed.getWindowSize();

                if (feed.hasVariableWindowSize() != null && feed.hasVariableWindowSize()) {
                    List<String> lastFileEntries = FileHelper.tail(filePath, 1);
                    int windowSizePositionInCSV = 5;
                    String recentWindow = null;
                    try {
                        recentWindow = lastFileEntries.get(0).split(";")[windowSizePositionInCSV];
                        recentWindowSize = Integer.parseInt(recentWindow);
                    } catch (Throwable th) {
                        LOGGER.fatal("Could not read window size from position " + windowSizePositionInCSV
                                + " (start with 0) in csv file for feedID " + feed.getId()
                                + ", using current window size instead.");
                    }
                }
                List<String> fileEntries = FileHelper.tail(filePath, recentWindowSize);

                List<String> newEntries = new ArrayList<String>();
                int newItems = 0;

                long pollTimestamp = feed.getLastPollTime().getTime();

                StringBuilder entryWarnings = new StringBuilder();

                LOGGER.debug("Feed entries: " + feedEntries.size());
                for (FeedItem item : feedEntries) {

                    StringBuilder fileEntry = new StringBuilder();
                    StringBuilder fileEntryID = new StringBuilder();

                    // title
                    if (item.getTitle() == null || item.getTitle().length() == 0) {
                        fileEntryID.append("\"###NO_TITLE###\";");
                    } else {
                        fileEntryID.append("\""
                                + StringHelper.removeControlCharacters(item.getTitle()).replaceAll("\"", "'")
                                        .replaceAll(";", "putSemicolonHere") + "\";");
                    }

                    // link
                    if (item.getLink() == null || item.getLink().length() == 0) {
                        fileEntryID.append("\"###NO_LINK###\";");
                    } else {
                        fileEntryID.append("\"" + StringHelper.trim(item.getLink()) + "\";");
                    }

                    // publish or updated date
                    if (item.getPublished() == null) {
                        entryWarnings.append("entry has no published date, setting default value for item: ")
                                .append(item).append("; ");
                        fileEntry.append(NO_TIMESTAMP).append(";");
                    } else {
                        fileEntry.append(item.getPublished().getTime()).append(";");
                    }

                    fileEntry.append(pollTimestamp).append(";");
                    fileEntry.append(item.getHash()).append(";");
                    fileEntry.append(fileEntryID);
                    fileEntry.append(feed.getWindowSize()).append(";");
                    // ignore entry size, we can get it later from *.gz

                    // add the entry only if it doesn't exist yet in the file: title and link are the comparison key
                    // Why not also using publish timestamp? We currently ignore on-the-fly generated timestamps, is
                    // this what we want? -- Sandro
                    boolean contains = false;
                    for (String savedFileEntry : fileEntries) {
                        // ignore first+second timestamp
                        if (savedFileEntry.substring(savedFileEntry.indexOf(";", savedFileEntry.indexOf(";") + 1) + 1)
                                .startsWith(fileEntryID.toString())) {
                            contains = true;
                            break;

                        }
                    }

                    if (!contains) {
                        newEntries.add(fileEntry.toString());
                        newItems++;
                    }

                }

                if (entryWarnings.length() > 0) {
                    FeedReader.LOGGER.warn(entryWarnings);
                }

                feed.incrementNumberOfItemsReceived(newItems);

                // if all entries are new, we might have checked to late and missed some entries, we mark that by a
                // special line
                if (newItems == feedEntries.size() && feed.getChecks() > 1 && newItems > 0) {
                    feed.increaseMisses();
                    newEntries.add("MISS;;;;;");
                    LOGGER.fatal("MISS: " + feed.getFeedUrl() + " (id " + +feed.getId() + ")" + ", checks: "
                            + feed.getChecks() + ", misses: " + feed.getMisses());
                }

                // save the complete feed gzipped in the folder if we found at least one new item
                if (newItems > 0) {

                    Collections.reverse(newEntries);

                    StringBuilder newEntryBuilder = new StringBuilder();
                    for (String string : newEntries) {
                        newEntryBuilder.append(string).append("\n");
                    }

                    DocumentRetriever documentRetriever = new DocumentRetriever();
                    String gzPath = folderPath + pollTimestamp + "_"
                            + DateHelper.getDatetime("yyyy-MM-dd_HH-mm-ss", pollTimestamp) + ".gz";
                    boolean gzWritten = documentRetriever.saveToFile(httpResult, gzPath, true);

                    LOGGER.debug("Saving new file content: " + newEntries.toString());
                    // FileHelper.prependFile(filePath, newEntries.toString());
                    boolean fileWritten = FileHelper.appendFile(filePath, newEntryBuilder);

                    if (!gzWritten || !fileWritten) {
                        success = false;
                    }
                }

                processPollMetadata(feed, httpResult, newItems);


                
                LOGGER.debug("added " + newItems + " new posts to file " + filePath + " (feed: " + feed.getId() + ")");

                return success;
            }

            /**
             * Write everything that we can't parse to a gz file.
             * All data written to gz file is taken from httpResult, the Feed is taken to determine the path and
             * filename.
             */
            @Override
            public boolean performActionOnError(Feed feed, HttpResult httpResult) {

                long pollTimestamp = System.currentTimeMillis();
                boolean success = false;
                boolean folderCreated = DatasetCreator.createDirectoriesAndCSV(feed);

                if (folderCreated) {
                    String folderPath = DatasetCreator.getFolderPath(feed.getId());
                    String gzPath = folderPath + pollTimestamp + "_"
                            + DateHelper.getDatetime("yyyy-MM-dd_HH-mm-ss", pollTimestamp) + "_unparsable.gz";

                    DocumentRetriever documentRetriever = new DocumentRetriever();
                    success = documentRetriever.saveToFile(httpResult, gzPath, true);
                    if (success) {
                        LOGGER.debug("Saved unparsable feed to: " + gzPath);
                    } else {
                        LOGGER.error("Could not save unparsable feed to: " + gzPath);
                    }
                }

                processPollMetadata(feed, httpResult, null);

                return success;
            }


            /**
             * FIXME put all Data to PollMetaInformation, write to database.
             * 
             * @param feed
             * @param httpResult
             * @param newItems
             */
            private void processPollMetadata(Feed feed, HttpResult httpResult, Integer newItems) {
                // write poll metadata

                PollMetaInformation pollMetaInfo = new PollMetaInformation();

                pollMetaInfo.setFeedID(feed.getId());
                pollMetaInfo.setPollTimestamp(feed.getLastPollTime());
                pollMetaInfo.setHttpETag(httpResult.getHeaderString("ETag"));

                StringBuilder metadata = new StringBuilder();

                metadata.append("httpDate=").append(httpResult.getHeaderString("Date"));
                metadata.append("httpLastModified=").append(httpResult.getHeaderString("Last-Modified"));
                metadata.append("httpExpires=").append(httpResult.getHeaderString("Expires"));
                metadata.append("httpTTL=").append(httpResult.getHeaderString("TTL"));
                metadata.append("newestItemTimestamp=").append(feed.getLastFeedEntry()); // FIXME
                metadata.append("numberNewItems=").append(newItems);
                metadata.append("windowSize=").append(feed.getWindowSize());

                // LOGGER.info(metadata);
            }

        };

        feedChecker.setFeedProcessingAction(fpa);

        LOGGER.debug("start reading feeds");
        feedChecker.startContinuousReading();
    }

    /**
     * @param feedID
     * @param safeFeedName
     * @return
     */
    public static String getCSVFilePath(int feedID, String safeFeedName) {
        return getFolderPath(feedID) + feedID + "_" + safeFeedName + ".csv";
    }

    /**
     * @param feedID
     * @return
     */
    public static String getFolderPath(int feedID) {
        return DATASET_PATH + getSlice(feedID) + System.getProperty("file.separator") + feedID
                + System.getProperty("file.separator");
    }

    /**
     * @param feedID
     * @return
     */
    public static int getSlice(int feedID) {
        return (int) Math.floor(feedID / 1000.0);
    }

    /**
     * @param feed
     * @return
     */
    public static String getSafeFeedName(String feedURL) {
        return StringHelper.makeSafeName(feedURL.replaceFirst("http://www.", "").replaceFirst("www.", ""), 30);
    }

    /**
     * Create the directories and the csv file for that feed if they do not exist.
     * 
     * @param feed The feed to create the directories and the csv file for.
     * @return <code>true</code> if folders and file were created or already existed, false on every error.
     */
    public static boolean createDirectoriesAndCSV(Feed feed) {

        boolean success = true;
        // get the path of the feed's folder and csv file

        String csvFilePath = DatasetCreator.getCSVFilePath(feed.getId(),
                DatasetCreator.getSafeFeedName(feed.getFeedUrl()));

        File postEntryFile = new File(csvFilePath);
        if (!postEntryFile.exists()) {
            boolean directoriesCreated = new File(postEntryFile.getParent()).mkdirs();
            try {
                if (directoriesCreated) {
                    success = postEntryFile.createNewFile();
                } else {
                    LOGGER.error("could not create the directories " + csvFilePath);
                    success = false;
                }
            } catch (IOException e) {
                LOGGER.error("could not create the file " + csvFilePath);
                success = false;
            }
        }
        return success;
    }

    /**
     * Detects system limitations like number of file descriptors that might cause trouble.
     */
    private void detectSystemLimitations() {
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        boolean checkLimitations = CHECK_SYSTEM_LIMITATIONS_DEFAULT;
        if (config != null) {
            checkLimitations = config.getBoolean("feedReader.checkSystemLimitations", CHECK_SYSTEM_LIMITATIONS_DEFAULT);
        }

        if (!checkLimitations) {
            LOGGER.warn("You skipped checking system for limitations. Good luck!");
            return;
        }
        String stdErrorMsg = "Make sure you have at least 20 times more file handles than FeedReader-threads.\n"
                + "Check palladian.properties > feedReader.threadPoolSize to get number of threads.\n"
                + "Run ulimit -n in a terminal to see the current soft limit of file descriptors for one session.\n"
                + "Run cat /proc/sys/fs/file-max to display maximum number of open file descriptors.\n"
                + "To increase the number of file descriptors, modify /etc/security/limits.conf (su required), add\n"
                + "<username> soft nofile <minimum-required-size>\n"
                + "<username> hard nofile <minimum-required-size>+1024\n"
                + "example"
                + "feeduser soft nofile 31744\n"
                + "feeduser hard nofile 32768\n"
                + "Restart your system afterwards or find out which process needs to be restartet to let the changes take effect.\n"
                + "See http://www.cyberciti.biz/faq/linux-increase-the-maximum-number-of-open-files/ for more details.";

        // detect operating system
        String os = System.getProperty("os.name");
        if (os.equalsIgnoreCase("linux")) {

            // TODO: use new ProcessHelper class
            // get available number of file handles for one UNIX terminal
            String input = "";
            try {
                Process process = Runtime.getRuntime().exec("/bin/sh ulimit -n");
                BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
                String line = null;
                while ((line = in.readLine()) != null) {
                    input += line;
                }
            } catch (IOException e) {
                LOGGER.error("Could not get number of available file handles: " + e.getLocalizedMessage());
            }
            int fileDescriptors = 0;
            if (!input.equals("")) {
                try {
                    LOGGER.info("ulimit -n: " + input);
                    fileDescriptors = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    LOGGER.fatal("Could not process number of available file handles: " + e.getLocalizedMessage()
                            + "\n" + stdErrorMsg);
                }
            }
            if (fileDescriptors <= 0) {
                LOGGER.fatal("Illegal number of file descriptors: " + fileDescriptors + ". " + stdErrorMsg);
                return;
            }

            // get thread pool size
            int threadPoolSize = 0;
            if (config != null) {
                threadPoolSize = config.getInteger("feedReader.threadPoolSize", FeedReader.DEFAULT_THREAD_POOL_SIZE);
            }

            /**
             * Stop executing if not enough file descriptors! Proceeding could cause serious trouble like an incomplete
             * data set.
             */
            if (threadPoolSize * FILE_HANDLES_PER_TASK <= fileDescriptors) {
                LOGGER.fatal("More file handles required! \n" + "threadPoolSize=" + threadPoolSize
                        + ", available file descriptors=" + fileDescriptors
                        + ", minimum required file descriptors would be " + threadPoolSize * FILE_HANDLES_PER_TASK
                        + "\n" + stdErrorMsg);
                System.exit(-1);
            }

        } else {
            // be carful! Windows 7 has no Limit, Win XP SP2 has a limit of concurrent connections, different
            // tools do not work, see (http://www.lvllord.de/), XP SP2 also has a limit of file handles
            // (C:\windows\system32\CONFIG.NT FILES=xx)
            LOGGER.info("It seems that you are running ths application on a non-linux machine. Make sure you have enough file descriptors :)");
        }
    }

    /**
     * Run creation of the feed dataset from all feeds in the database if possible.
     * 
     * @param args Command line arguments are ignored.
     */
    public static void main(String[] args) {

        // System.out.println(System.getProperty("os.name"));
        //
        // System.exit(0);

        DatasetCreator dc = new DatasetCreator();
        dc.createDataset();
        // DatasetCreator.renewFileIDs();
        // DatasetCreator.cleanUp(true);
        // DatasetCreator.combineFeedHistories();
        // dc.addFeedMetaInformation();

    }

}