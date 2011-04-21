package ws.palladian.retrieval.feeds.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
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
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedProcessingAction;
import ws.palladian.retrieval.feeds.FeedReader;
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
 * TIMESTAMP;"TITLE";LINK
 * </pre>
 * <p>
 * If the creator finds a completely new window it must assume that it missed some entries and adds a line containing
 * <tt>MISS;MISS;MISS</tt>.
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
    protected static final String DATASET_PATH = "data" + System.getProperty("file.separator") + "datasets"
            + System.getProperty("file.separator") + "feedPosts" + System.getProperty("file.separator");

    /** We need this many file handles to process one FeedTask. */
    public static final int FILE_HANDLES_PER_TASK = 20;

    public static final boolean CHECK_SYSTEM_LIMITATIONS_DEFAULT = true;

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

        FeedStore feedStore = new FeedDatabase();

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
            public void performAction(Feed feed) {

                // get the filename of the feed
                String safeFeedName = StringHelper.makeSafeName(feed.getFeedUrl().replaceFirst("http://www.", "")
                        .replaceFirst("www.", ""), 30);

                int slice = (int) Math.floor(feed.getId() / 1000.0);

                String folderPath = DATASET_PATH + slice + "/" + feed.getId() + "/";
                String filePath = folderPath + feed.getId() + "_" + safeFeedName + ".csv";
                LOGGER.debug("saving feed to: " + filePath);

                // get entries from the file
                File postEntryFile = new File(filePath);
                if (!postEntryFile.exists()) {
                    boolean directoriesCreated = new File(postEntryFile.getParent()).mkdirs();
                    try {
                        if (directoriesCreated) {
                            postEntryFile.createNewFile();
                        } else {
                            LOGGER.error("could not create the directories " + filePath);
                        }
                    } catch (IOException e) {
                        LOGGER.error("could not create the file " + filePath);
                    }
                }
                List<String> fileEntries = FileHelper.readFileToArray(filePath);

                // get all posts in the feed as timestamp;headline;link
                List<FeedItem> feedEntries = feed.getItems();

                if (feedEntries == null) {
                    LOGGER.warn("no feed entries for " + feed.getFeedUrl());
                    return;
                }

                // calculate size of feed header and footer, which should always stay the same.
                // long summedFeedEntrySize = 0;
                // for (FeedItem entry : feedEntries) {
                // String entryPlainXML = entry.getRawMarkup();
                // Integer entrySize = entryPlainXML.getBytes().length;
                // summedFeedEntrySize += entrySize;
                // }

                // LOGGER.info("feed: "+feed);
                // LOGGER.debug("feed.getPlainXML: "+feed.getPlainXML());
                // String feedPlainXML = feed.getRawMarkup();
                // Integer feedSize = feedPlainXML.getBytes().length;
                // long feedContainerSize = feedSize - summedFeedEntrySize;
                // we dont need the size since we store everything in *.gz
                long feedContainerSize = -1L;

                StringBuilder newEntries = new StringBuilder();
                int newItems = 0;

                StringBuilder entryWarnings = new StringBuilder();

                LOGGER.debug("Feed entries: " + feedEntries.size());
                for (FeedItem entry : feedEntries) {

                    if (entry == null || entry.getPublished() == null) {
                        entryWarnings.append("entry has no published date, ignore it: ").append(entry).append("; ");
                        continue;
                    }

                    String fileEntry = "";
                    String fileEntryID = "";

                    if (entry.getTitle() == null || entry.getTitle().length() == 0) {
                        fileEntryID += "\"###NO_TITLE###\";";
                    } else {
                        fileEntryID += "\""
                                + StringHelper.removeControlCharacters(entry.getTitle()).replaceAll("\"", "'")
                                        .replaceAll(";", "putSemicolonHere") + "\";";
                    }
                    fileEntryID += "\"" + StringHelper.trim(entry.getLink()) + "\";";
                    fileEntry = entry.getPublished().getTime() + ";" + fileEntryID;
                    // ignore entry size, we can get it later from *.gz
                    fileEntry += "-1;";
                    // fileEntry += entry.getRawMarkup().getBytes().length + ";";
                    fileEntry += feedContainerSize + ";";
                    fileEntry += feedEntries.size();

                    // add the entry only if it doesn't exist yet in the file: title and link are the comparison key
                    boolean contains = false;
                    for (String savedFileEntry : fileEntries) {
                        if (savedFileEntry.substring(14).startsWith(fileEntryID)) {
                            contains = true;
                            break;
                        }
                    }

                    if (!contains) {
                        newEntries.append(fileEntry).append("\n");
                        newItems++;
                    }

                }

                if (entryWarnings.length() > 0) {
                    FeedReader.LOGGER.warn(entryWarnings);
                }

                // if all entries are new, we might have checked to late and missed some entries, we mark that by a
                // special line
                if (newItems == feedEntries.size() && feed.getChecks() > 1 && newItems > 0) {
                    feed.increaseMisses();
                    newEntries.append("MISS;MISS;MISS;MISS;MISS;MISS").append("\n");
                    LOGGER.fatal("MISS: " + feed.getFeedUrl() + "(" + +feed.getId() + ")" + ", checks: "
                            + feed.getChecks());
                }

                // save the complete feed gzipped in the folder if we found at least one new item
                if (newItems > 0) {
                    DocumentRetriever documentRetriever = new DocumentRetriever();
                    documentRetriever.downloadAndSave(feed.getFeedUrl(),
                            folderPath + DateHelper.getCurrentDatetime("yyyy-MM-dd_HH-mm-ss") + ".gz", true);
                    LOGGER.debug("Saving new file content: " + newEntries.toString());
                    FileHelper.prependFile(filePath, newEntries.toString());
                }

                feed.freeMemory();
                feed.setLastHeadlines("");

                LOGGER.debug("added " + newItems + " new posts to file " + filePath + " (feed: " + feed.getId() + ")");

            }
        };

        feedChecker.setFeedProcessingAction(fpa);

        LOGGER.debug("start reading feeds");
        feedChecker.startContinuousReading();
    }

    /**
     * Detects system limitations like number of file descriptors that might cause trouble.
     */
    private void detectSystemLimitations() {
        PropertiesConfiguration config = ConfigHolder.getInstance().getConfig();
        boolean checkLimitations = CHECK_SYSTEM_LIMITATIONS_DEFAULT;
        if (config != null) {
            checkLimitations= config.getBoolean("feedReader.checkSystemLimitations", CHECK_SYSTEM_LIMITATIONS_DEFAULT);
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
                Process process = Runtime.getRuntime().exec("ulimit -n");
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
            LOGGER.info("It seems that you are running ths application on a non-linux machine. Make sure you have enough file descriptors :)");
        }
    }

    /**
     * Run creation of the feed dataset from all feeds in the database if possible.
     * 
     * @param args Command line arguments are ignored.
     */
    public static void main(String[] args) {

        DatasetCreator dc = new DatasetCreator();
        dc.createDataset();
        // DatasetCreator.renewFileIDs();
        // DatasetCreator.cleanUp(true);
        // DatasetCreator.combineFeedHistories();
        // dc.addFeedMetaInformation();

    }

}