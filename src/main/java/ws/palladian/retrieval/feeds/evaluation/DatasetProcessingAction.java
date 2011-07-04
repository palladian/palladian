package ws.palladian.retrieval.feeds.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import org.apache.log4j.Logger;

import ws.palladian.helper.FileHelper;
import ws.palladian.helper.HTTPHelper;
import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.FeedProcessingAction;
import ws.palladian.retrieval.feeds.FeedRetriever;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;
import ws.palladian.retrieval.feeds.persistence.FeedStore;

class DatasetProcessingAction extends FeedProcessingAction {
    
    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(DatasetProcessingAction.class);
    
    private final FeedStore feedStore;
    
    DatasetProcessingAction(FeedStore feedStore) {
        this.feedStore = feedStore;
    }
    
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
        String folderPath = DatasetCreator.getFolderPath(feed.getId());
        String filePath = DatasetCreator.getCSVFilePath(feed.getId(), DatasetCreator.getSafeFeedName(feed.getFeedUrl()));
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
                LOGGER.error("Could not read window size from position " + windowSizePositionInCSV
                        + " (start with 0) in csv file for feedID " + feed.getId()
                        + ", using current window size instead.");
            }
        }
        List<String> fileEntries = FileHelper.tail(filePath, recentWindowSize);

        List<String> newEntries = new ArrayList<String>();
        int newItems = 0;

        long pollTimestamp = feed.getLastPollTime().getTime();

        // get all hashes from items stored to file
        HashSet<String> savedFileEntryHashes = new HashSet<String>();
        for (String savedFileEntry : fileEntries) {
            savedFileEntryHashes.add(savedFileEntry.split(";")[2]);
        }

        // StringBuilder entryWarnings = new StringBuilder();

        LOGGER.debug("Feed entries: " + feedEntries.size());
        for (FeedItem item : feedEntries) {

            String itemHash = item.getHash();
            if (savedFileEntryHashes.contains(itemHash)) {
                // this item is already known, do nothing with it.
                continue;
            }

            String fileEntry = buildCsvLine(item);

            newEntries.add(fileEntry);
            newItems++;

        }

        // if (entryWarnings.length() > 0) {
        // FeedReader.LOGGER.warn(entryWarnings);
        // }

        feed.incrementNumberOfItemsReceived(newItems);

        // if all entries are new, we might have checked to late and missed some entries, we mark that by a
        // special line
        if (newItems == feedEntries.size() && feed.getChecks() > 1 && newItems > 0) {
            feed.increaseMisses();
            newEntries.add("MISS;;;;;;");
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

    private static String buildCsvLine(FeedItem item) {
        
        // build csv line for new entry
        StringBuilder fileEntry = new StringBuilder();

        // item publish or updated date
        if (item.getPublished() == null) {
            fileEntry.append(DatasetCreator.NO_TIMESTAMP).append(";");
        } else {
            fileEntry.append(item.getPublished().getTime()).append(";");
        }

        // poll timestamp
        fileEntry.append(item.getFeed().getLastPollTime().getTime()).append(";");

        // item hash
        fileEntry.append(item.getHash()).append(";");

        // item title
        if (item.getTitle() == null || item.getTitle().length() == 0) {
            fileEntry.append(DatasetCreator.NO_TITLE_REPLACEMENT).append(";");
        } else {
            fileEntry.append("\"");
            fileEntry.append(StringHelper.cleanStringToCsv(item.getTitle()));
            fileEntry.append("\";");
        }

        // item link
        if (item.getLink() == null || item.getLink().length() == 0) {
            fileEntry.append(DatasetCreator.NO_LINK_REPLACEMENT).append(";");
        } else {
            fileEntry.append("\"");
            fileEntry.append(StringHelper.cleanStringToCsv(item.getLink()));
            fileEntry.append("\";");
        }

        // window size
        fileEntry.append(item.getFeed().getWindowSize()).append(";");
        return fileEntry.toString();
    }

    /**
     * Write poll meta information to db.
     */
    @Override
    public boolean performActionOnUnmodifiedFeed(Feed feed, HttpResult httpResult) {

        return processPollMetadata(feed, httpResult, null);
    }

    /**
     * Write poll meta information to db.
     */
    @Override
    public boolean performActionOnHighHttpStatusCode(Feed feed, HttpResult httpResult) {

        return processPollMetadata(feed, httpResult, null);
    }

    /**
     * Write everything that we can't parse to a gz file.
     * All data written to gz file is taken from httpResult, the Feed is taken to determine the path and
     * filename.
     */
    @Override
    public boolean performActionOnException(Feed feed, HttpResult httpResult) {

        long pollTimestamp = feed.getLastPollTime().getTime();
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
     * Put data to PollMetaInformation, write to database.
     * 
     * @param feed
     * @param httpResult
     * @param newItems
     * @return
     */
    private boolean processPollMetadata(Feed feed, HttpResult httpResult, Integer newItems) {

        PollMetaInformation pollMetaInfo = new PollMetaInformation();

        pollMetaInfo.setFeedID(feed.getId());
        pollMetaInfo.setPollTimestamp(feed.getLastPollTime());
        pollMetaInfo.setHttpETag(httpResult.getHeaderString("ETag"));
        pollMetaInfo.setHttpDate(HTTPHelper.getDateFromHeader(httpResult, "Date"));
        pollMetaInfo.setHttpLastModified(HTTPHelper.getDateFromHeader(httpResult, "Last-Modified"));
        pollMetaInfo.setHttpExpires(HTTPHelper.getDateFromHeader(httpResult, "Expires"));
        pollMetaInfo.setHttpTTL(StringHelper.toLong(httpResult.getHeaderString("TTL")));
        pollMetaInfo.setNewestItemTimestamp(feed.getLastFeedEntry());
        pollMetaInfo.setNumberNewItems(newItems);
        pollMetaInfo.setWindowSize(feed.getWindowSize());
        pollMetaInfo.setHttpStatusCode(httpResult.getStatusCode());

        return feedStore.addFeedPoll(pollMetaInfo);
    }
    
    public static void main(String[] args) throws Exception {
        FeedRetriever fr = new FeedRetriever();
        Feed feed = fr.getFeed("http://www.d3p.co.jp/rss/mobile.rdf");
        feed.setLastPollTime(new Date());
        FeedItem item = feed.getItems().get(0);
        System.out.println(buildCsvLine(item));
    }

}
