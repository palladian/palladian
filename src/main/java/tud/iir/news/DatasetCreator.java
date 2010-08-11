package tud.iir.news;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import tud.iir.helper.FileHelper;
import tud.iir.helper.StringHelper;

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
    protected static final String DATASET_PATH = "data/datasets/feedPosts/";


    /**
     * Start creating the dataset.
     */
    public void createDataset() {

        FeedStore feedStore = FeedDatabase.getInstance();

        // all feeds need to be classified in advance to filter them accordingly
        //FeedClassifier.classifyFeedInStore(feedStore);

        FeedChecker feedChecker = new FeedChecker(feedStore);

        feedChecker.setCheckApproach(CheckApproach.CHECK_FIXED, true);
        // feedChecker.setCheckInterval(2);
        
        // create the dataset only with feeds that are parsable, have at least one entry, and are alive
        Collection<Integer> updateClasses = new HashSet<Integer>();
        updateClasses.add(FeedClassifier.CLASS_ZOMBIE);
        updateClasses.add(FeedClassifier.CLASS_SPONTANUOUS);
        updateClasses.add(FeedClassifier.CLASS_SLICED);
        updateClasses.add(FeedClassifier.CLASS_SINGLE_ENTRY);
        updateClasses.add(FeedClassifier.CLASS_ON_THE_FLY);
        updateClasses.add(FeedClassifier.CLASS_CONSTANT);
        updateClasses.add(FeedClassifier.CLASS_CHUNKED);
        //feedChecker.filterFeeds(updateClasses);

        FeedProcessingAction fpa = new FeedProcessingAction() {

            @Override
            public void performAction(Feed feed) {
//                System.out.println("do stuff with " + feed.getFeedUrl());
//                System.out.println("::: check interval: " + feed.getMaxCheckInterval() + ", checks: "
//                        + feed.getChecks());

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
                List<FeedEntry> feedEntries = feed.getEntries();

                if (feedEntries == null) {
                    LOGGER.warn("no feed entries for " + feed.getFeedUrl());
                    return;
                }

                // Calculating size of feed header and footer, which should always stay the same.
                long summedFeedEntrySize = 0;
                for(FeedEntry entry:feedEntries) {
                    String entryPlainXML = entry.getPlainXML();
                    Integer entrySize = entryPlainXML.getBytes().length;
                    summedFeedEntrySize += entrySize;
                }
                
                //LOGGER.info("feed: "+feed);
                //LOGGER.debug("feed.getPlainXML: "+feed.getPlainXML());
                String feedPlainXML = feed.getPlainXML();
                Integer feedSize = feedPlainXML.getBytes().length;
                long feedContainerSize = feedSize-summedFeedEntrySize;
                
                StringBuilder newEntries = new StringBuilder();
                int newPosts = 0;

                for (FeedEntry entry : feedEntries) {

                    if (entry == null || entry.getPublished() == null) {
                        LOGGER.warn("entry has no published date, ignore it: " + entry);
                        continue;
                    }

                    String fileEntry = "";

                    fileEntry += entry.getPublished().getTime() + ";";
                    if (entry.getTitle() == null || entry.getTitle().length() == 0) {
                    	fileEntry += "\"###NO_TITLE###\";";
                    } else {
                    	fileEntry += "\"" + entry.getTitle().replaceAll("\"", "'").replaceAll(";", "putSemicolonHere") + "\";";
                    }                    
                    fileEntry += "\"" + entry.getLink() + "\";";
                     fileEntry += entry.getPlainXML().getBytes().length + ";";
                     fileEntry += feedContainerSize+";";
                     fileEntry += feedEntries.size();

                    // add the entry only if it doesn't exist yet in the file
                    if (!fileEntries.contains(fileEntry)) {

                        newEntries.append(fileEntry).append("\n");
                        newPosts++;

                    }
                    
                }
                
                // if all entries are new, we might have checked to late and missed some entries, we mark that by a
                // special line
                if (newPosts == feedEntries.size() && feed.getChecks() > 1 && newPosts > 0) {
                    newEntries.append("MISS;MISS;MISS;MISS;MISS;MISS").append("\n");
                    LOGGER.fatal("MISS: " + feed.getFeedUrl() + ", checks: " + feed.getChecks());
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
     * Remove all empty files from dataset folder.
     */
    public void cleanUp() {
        File[] files = FileHelper.getFiles(DATASET_PATH);
        for (File file : files) {
            if (file.length() == 0) {
                file.delete();
            }
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
        dc.cleanUp();
        dc.addFeedMetaInformation();

    }

    /**
     * <p>
     * 
     * </p>
     *
     */
    private void addFeedMetaInformation() {
        // TODO Auto-generated method stub
        
    }

}
