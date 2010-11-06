package tud.iir.news.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;
import tud.iir.helper.MathHelper;
import tud.iir.helper.StopWatch;
import tud.iir.helper.StringHelper;
import tud.iir.news.Feed;
import tud.iir.news.FeedClassifier;
import tud.iir.news.FeedDatabase;
import tud.iir.news.FeedItem;
import tud.iir.news.FeedProcessingAction;
import tud.iir.news.FeedReader;
import tud.iir.news.FeedStore;
import tud.iir.news.UpdateStrategy;

/**
 * <p>
 * Creates a dataset of feed posts.
 * </p>
 * <p>
 * For each feed, a csv file is created in the data/datasets/feedPosts/ folder. Each file contains all distinct posts
 * collected over a period of time. Each file follows the follwowing layout:<br>
 * 
 * TIMESTAMP;"TITLE";LINK
 * </p>
 * <p>
 * TODO The first line of the file contains meta information:<br>
 * FEED_ID;FEED_URL;NUMBER_OF_ENTRIES(Window Size);AVERAGE_SIZE;FEED_UPDATE_CLASS
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public class DatasetCreator {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetCreator.class);

    /** Path to the folder where the dataset is stored. */
    protected static final String DATASET_PATH = "G:\\Projects\\Programming\\Other\\feedPosts\\";

    /**
     * Start creating the dataset.
     */
    public void createDataset() {

        FeedStore feedStore = FeedDatabase.getInstance();

        // all feeds need to be classified in advance to filter them accordingly
        // FeedClassifier.classifyFeedInStore(feedStore);

        FeedReader feedChecker = new FeedReader(feedStore);

        FeedReaderEvaluator.setBenchmarkPolicy(FeedReaderEvaluator.BENCHMARK_OFF);

        feedChecker.setCheckApproach(UpdateStrategy.UPDATE_FIXED, true);
        feedChecker.setCheckInterval(1);

        // create the dataset only with feeds that are parsable, have at least one entry, and are alive
        Collection<Integer> updateClasses = new HashSet<Integer>();
        updateClasses.add(FeedClassifier.CLASS_ZOMBIE);
        updateClasses.add(FeedClassifier.CLASS_SPONTANEOUS);
        updateClasses.add(FeedClassifier.CLASS_SLICED);
        updateClasses.add(FeedClassifier.CLASS_SINGLE_ENTRY);
        updateClasses.add(FeedClassifier.CLASS_ON_THE_FLY);
        updateClasses.add(FeedClassifier.CLASS_CONSTANT);
        updateClasses.add(FeedClassifier.CLASS_CHUNKED);
        // feedChecker.filterFeeds(updateClasses);

        FeedProcessingAction fpa = new FeedProcessingAction() {

            @Override
            public void performAction(Feed feed) {
                // System.out.println("do stuff with " + feed.getFeedUrl());
                // System.out.println("::: check interval: " + feed.getMaxCheckInterval() + ", checks: "
                // + feed.getChecks());

                // get the filename of the feed
                String safeFeedName = StringHelper.makeSafeName(feed.getFeedUrl().replaceFirst("http://www.", "")
                        .replaceFirst("www.", ""), 30);
                String filePath = DATASET_PATH + feed.getId() + "_" + safeFeedName + ".csv";

                // get entries from the file
                File postEntryFile = new File(filePath);
                if (!postEntryFile.exists()) {
                    new File(postEntryFile.getParent()).mkdirs();
                    try {
                        postEntryFile.createNewFile();
                    } catch (IOException e) {
                        LOGGER.error("could not create the file " + filePath);
                    }
                }
                List<String> fileEntries = FileHelper.readFileToArray(filePath);

                // get all posts in the feed as timestamp;headline;link
                List<FeedItem> feedEntries = feed.getEntries();

                if (feedEntries == null) {
                    LOGGER.warn("no feed entries for " + feed.getFeedUrl());
                    return;
                }

                // Calculating size of feed header and footer, which should always stay the same.
                long summedFeedEntrySize = 0;
                for (FeedItem entry : feedEntries) {
                    String entryPlainXML = entry.getPlainXML();
                    Integer entrySize = entryPlainXML.getBytes().length;
                    summedFeedEntrySize += entrySize;
                }

                // LOGGER.info("feed: "+feed);
                // LOGGER.debug("feed.getPlainXML: "+feed.getPlainXML());
                String feedPlainXML = feed.getPlainXML();
                Integer feedSize = feedPlainXML.getBytes().length;
                long feedContainerSize = feedSize - summedFeedEntrySize;

                StringBuilder newEntries = new StringBuilder();
                int newPosts = 0;

                for (FeedItem entry : feedEntries) {

                    if (entry == null || entry.getPublished() == null) {
                        LOGGER.warn("entry has no published date, ignore it: " + entry);
                        continue;
                    }

                    String fileEntry = "";
                    String fileEntryID = "";

                    if (entry.getTitle() == null || entry.getTitle().length() == 0) {
                        fileEntryID += "\"###NO_TITLE###\";";
                    } else {
                        fileEntryID += "\""
                                + entry.getTitle().replaceAll("\"", "'").replaceAll(";", "putSemicolonHere") + "\";";
                    }
                    fileEntryID += "\"" + StringHelper.trim(entry.getLink()) + "\";";
                    fileEntry = entry.getPublished().getTime() + ";" + fileEntryID;
                    fileEntry += entry.getPlainXML().getBytes().length + ";";
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
                        newPosts++;
                    }

                }

                // if all entries are new, we might have checked to late and missed some entries, we mark that by a
                // special line
                if (newPosts == feedEntries.size() && feed.getChecks() > 1 && newPosts > 0) {
                    newEntries.append("MISS;MISS;MISS;MISS;MISS;MISS").append("\n");
                    LOGGER.fatal("MISS: " + feed.getFeedUrl() + "(" + +feed.getId() + ")" + ", checks: "
                            + feed.getChecks());
                }

                try {
                    FileHelper.prependFile(filePath, newEntries.toString());
                } catch (IOException e) {
                    LOGGER.error("could not prepend new file entries (" + newEntries + ") to " + filePath);
                }

                feed.freeMemory();
                feed.setLastHeadlines("");

                LOGGER.debug("added " + newPosts + " new posts to file " + filePath + " (feed: " + feed.getId() + ")");

            }
        };

        feedChecker.setFeedProcessingAction(fpa);

        LOGGER.debug("start reading feeds");
        feedChecker.startContinuousReading();

    }

    /**
     * <p>We combine all feed histories into one file which we can then import into a database to generate statistics.</p>
     */
    public static void combineFeedHistories() {

        StopWatch sw = new StopWatch();

        String cleanPath = DATASET_PATH + "clean\\";
        
        FileWriter fileWriter = null;
		try {
			fileWriter = new FileWriter(cleanPath+"all.csv");
		} catch (IOException e1) {
			LOGGER.error(e1.getMessage());
		}
        
		int c = 0;
        File[] files = FileHelper.getFiles(cleanPath);
        for (File file : files) {
        	
        	String feedID = file.getName().substring(0,file.getName().indexOf("_"));
        	
        	List<String> contents = FileHelper.readFileToArray(file);
        	
        	for (String line : contents) {
        	
	        	try {
	        		fileWriter.write(feedID+";");
	                fileWriter.write(line);
	                fileWriter.flush();	                
	            } catch (IOException e) {
	                LOGGER.error(file + ", " + e.getMessage());
	            }
            
        	}
        	
        	c++;
            LOGGER.info("percent done: " + MathHelper.round(100*c/(double)files.length, 2));            
        }
        
        try {
        	fileWriter.close();
        } catch (Exception e) {
        	LOGGER.error(e.getMessage());
		}
        
        LOGGER.info("all files combined to all.csv in " + sw.getElapsedTimeString());
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
                    System.out.println(MathHelper.round((double) 100 * c / fileCount, 2)
                            + "% of the files cleansed");
                }

            }
        }

        // remove empty files again because there might be empty clean files now
        files = FileHelper.getFiles(cleanPath);
        for (File file : files) {

            if (file.length() == 0) {
                file.delete();
                deleteCount++;
            }
        }

        LOGGER.info("finished in " + sw.getElapsedTimeString() + ", deleted " + deleteCount + " files");
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
            System.out.println(fileNameRealID);
            // FileHelper.rename(file, fileNameRealID);

            FileHelper.copyFile(cleanPath + file.getName(), cleanPath + fileNameRealID);
            FileHelper.delete(cleanPath + file.getName());

        }

    }

    /**
     * Run creation of the feed dataset from all feeds in the database if possible.
     * 
     * @param args Command line arguments are ignored.
     */
    public static void main(String[] args) {

        // DatasetCreator dc = new DatasetCreator();
        // dc.createDataset();
        // DatasetCreator.renewFileIDs();
        DatasetCreator.cleanUp(true);
        // DatasetCreator.combineFeedHistories();
        // dc.addFeedMetaInformation();

    }

}
