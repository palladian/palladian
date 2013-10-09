package ws.palladian.retrieval.feeds.evaluation.datasetPostprocessing.gzProcessing;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.feeds.DefaultFeedProcessingAction;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.evaluation.DatasetCreator;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;
import ws.palladian.retrieval.feeds.persistence.FeedDatabase;
import ws.palladian.retrieval.helper.HttpHelper;

/**
 * TUDCS6 specific.<br />
 * Reconstruct the csv file from persisted gz files to eliminate false positive MISSes caused by altering window sizes
 * and wrong item hashes in case of seesionIDs in items' link and raw id attributes.
 * 
 * @author Sandro Reichert
 * 
 */
class SessionIdFixProcessingAction extends DefaultFeedProcessingAction {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SessionIdFixProcessingAction.class);

    private final FeedDatabase feedStore;

    SessionIdFixProcessingAction(FeedDatabase feedStore) {
        this.feedStore = feedStore;
    }

    @Override
    public boolean performAction(Feed feed, HttpResult httpResult) {

        boolean success = true;

        List<FeedItem> newFeedEntries = feed.getNewItems();

        // get the path of the feed's folder and csv file
        String csvFilePath = DatasetCreator.getCSVFilePath(feed.getId(), DatasetCreator.getSafeFeedName(feed.getFeedUrl()));
        // LOGGER.debug("saving feed to: " + filePath);
        success = DatasetCreator.createDirectoriesAndCSV(feed);

        List<String> newEntriesToWrite = new ArrayList<String>();
        int newItems = newFeedEntries.size();

        // LOGGER.debug("Feed entries: " + newFeedEntries.size());
        for (FeedItem item : newFeedEntries) {

            String fileEntry = buildCsvLine(item);
            newEntriesToWrite.add(fileEntry);
        }

        // if all entries are new, we might have checked to late and missed some entries, we mark that by a
        // special line
        if (newItems == feed.getWindowSize() && feed.getChecks() > 1 && newItems > 0) {
            feed.increaseMisses();
            newEntriesToWrite.add("MISS;;;;;;");
            // log to warn only
            LOGGER.warn("MISS: " + feed.getFeedUrl() + " (id " + +feed.getId() + ")" + ", checks: "
                    + feed.getChecks() + ", misses: " + feed.getMisses());
        }

        // add new items to csv
        if (newItems > 0 || feed.getChecks() == 1) {

            Collections.reverse(newEntriesToWrite);

            StringBuilder newEntryBuilder = new StringBuilder();
            for (String string : newEntriesToWrite) {
                newEntryBuilder.append(string).append("\n");
            }

            LOGGER.debug("Saving new file content: " + newEntriesToWrite.toString());
            boolean fileWritten = FileHelper.appendFile(csvFilePath, newEntryBuilder);

            if (!fileWritten) {
                success = false;
            }
        }

        boolean metadata = processPollMetadata(feed, httpResult, newItems);
        if (!metadata) {
            success = false;
        }

        LOGGER.debug("added " + newItems + " new posts to file " + csvFilePath + " (feed: " + feed.getId() + ")");

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
            fileEntry.append(item.getTitle().replace("\"", "'"));
            fileEntry.append("\";");
        }

        // item link
        if (item.getUrl() == null || item.getUrl().length() == 0) {
            fileEntry.append(DatasetCreator.NO_LINK_REPLACEMENT).append(";");
        } else {
            fileEntry.append("\"");
            fileEntry.append(item.getUrl().replace("\"", "'"));
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
    public boolean performActionOnError(Feed feed, HttpResult httpResult) {

        return processPollMetadata(feed, httpResult, null);
    }

    /**
     * Nothing to do, should never be called!
     */
    @Override
    public boolean performActionOnException(Feed feed, HttpResult httpResult) {
        LOGGER.error("performActionOnException is not implemented!");
        return false;
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

        long correctedTime = (long) ((Math.ceil(feed.getLastPollTime().getTime() / 1000)) * 1000);

        PollMetaInformation pollMetaInfo = feedStore.getFeedPoll(feed.getId(), new Timestamp(correctedTime));
        if (pollMetaInfo == null) {
            LOGGER.error("Could not load PollMetaInformation from DB for feed id " + feed.getId()
                    + " and pollTimestamp " + new Date(correctedTime) + ". PollMetaInformations has not been updated!");
            return false;
        }
        pollMetaInfo.setFeedID(feed.getId());
        pollMetaInfo.setHttpETag(httpResult.getHeaderString("ETag"));
        pollMetaInfo.setHttpDate(HttpHelper.getDateFromHeader(httpResult, "Date", true));
        pollMetaInfo.setHttpLastModified(HttpHelper.getDateFromHeader(httpResult, "Last-Modified", false));
        pollMetaInfo.setHttpExpires(HttpHelper.getDateFromHeader(httpResult, "Expires", false));
        pollMetaInfo.setNewestItemTimestamp(feed.getLastFeedEntry());
        pollMetaInfo.setNumberNewItems(newItems);
        pollMetaInfo.setWindowSize(feed.getWindowSize());
        pollMetaInfo.setHttpStatusCode(httpResult.getStatusCode());

        return feedStore.updateFeedPoll(pollMetaInfo);
    }

}
