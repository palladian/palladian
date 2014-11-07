package ws.palladian.retrieval.feeds.evaluation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.helper.io.FileHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.feeds.DefaultFeedProcessingAction;
import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;
import ws.palladian.retrieval.feeds.parser.FeedParser;
import ws.palladian.retrieval.feeds.parser.RomeFeedParser;
import ws.palladian.retrieval.feeds.persistence.FeedStore;
import ws.palladian.retrieval.helper.HttpHelper;

class DatasetProcessingAction extends DefaultFeedProcessingAction {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DatasetProcessingAction.class);

    private final FeedStore feedStore;

    DatasetProcessingAction(FeedStore feedStore) {
        this.feedStore = feedStore;
    }

    @Override
    public void onModified(Feed feed, HttpResult httpResult) {
        
        List<FeedItem> newFeedEntries = feed.getNewItems();

        // get the path of the feed's folder and csv file
        String folderPath = DatasetCreator.getFolderPath(feed.getId());
        String csvFilePath = DatasetCreator.getCSVFilePath(feed.getId(), DatasetCreator.getSafeFeedName(feed.getFeedUrl()));
        // LOGGER.debug("saving feed to: " + filePath);
        boolean createdDirectories = DatasetCreator.createDirectoriesAndCSV(feed);
        if (!createdDirectories) {
            throw new IllegalStateException("Error while creating directories");
        }

        List<String> newEntriesToWrite = new ArrayList<String>();
        int newItems = newFeedEntries.size();
        long pollTimestamp = feed.getLastPollTime().getTime();

        // LOGGER.debug("Feed entries: " + newFeedEntries.size());
        for (FeedItem item : newFeedEntries) {

            String fileEntry = buildCsvLine(item);
            newEntriesToWrite.add(fileEntry);
        }

        // if all entries are new, we might have checked to late and missed some entries, we mark that by a
        // special line
        // TODO feed.getChecks()>1 may be replaced by newItems<feed.getNumberOfItemsReceived() to avoid writing a MISS
        // if a feed was empty and we now found one or more items. We have to define the MISS. If we say we write a MISS
        // every time it can happen that we missed a item, feed.getChecks()>1 is correct. If we say there cant be a MISS
        // before we see the first item, feed.getChecks()>1 has to be replaced. -- Sandro 10.08.2011
        if (newItems == feed.getWindowSize() && feed.getChecks() > 1 && newItems > 0) {
            feed.increaseMisses();
            newEntriesToWrite.add("MISS;;;;;;");
            LOGGER.error("MISS: " + feed.getFeedUrl() + " (id " + +feed.getId() + ")" + ", checks: "
                    + feed.getChecks() + ", misses: " + feed.getMisses());
        }

        // save the complete feed gzipped in the folder if we found at least one new item or if its the first check
        if (newItems > 0 || feed.getChecks() == 1) {

            Collections.reverse(newEntriesToWrite);

            StringBuilder newEntryBuilder = new StringBuilder();
            for (String string : newEntriesToWrite) {
                newEntryBuilder.append(string).append("\n");
            }

            boolean gzWritten = writeGZ(httpResult, folderPath, pollTimestamp, "");

            LOGGER.debug("Saving new file content: " + newEntriesToWrite.toString());
            boolean fileWritten = FileHelper.appendFile(csvFilePath, newEntryBuilder);

            if (!gzWritten || !fileWritten) {
                throw new IllegalStateException("Error while writing gz or file.");
            }

            // there is sometimes a weird behavior of some feeds that suddenly seem to change their window size to zero.
            // In this case, we store the received content for debugging. -- Sandro 11.07.11
            // } else if (feed.getWindowSize() == 0 && feed.hasVariableWindowSize()) {
            // success = writeGZ(httpResult, folderPath, pollTimestamp, "_debug");
        }

        processPollMetadata(feed, httpResult, newItems);

        LOGGER.debug("added " + newItems + " new posts to file " + csvFilePath + " (feed: " + feed.getId() + ")");
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
            fileEntry.append(item.getTitle().replace("\"", "'")); // TODO clean string?
            fileEntry.append("\";");
        }

        // item link
        if (item.getUrl() == null || item.getUrl().length() == 0) {
            fileEntry.append(DatasetCreator.NO_LINK_REPLACEMENT).append(";");
        } else {
            fileEntry.append("\"");
            fileEntry.append(item.getUrl().replace("\"", "'")); // TODO clean string?
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
    public void onUnmodified(Feed feed, HttpResult httpResult) {
        processPollMetadata(feed, httpResult, null);
    }

    /**
     * Write poll meta information to db.
     */
    @Override
    public void onError(Feed feed, HttpResult httpResult) {
        processPollMetadata(feed, httpResult, null);
    }

    /**
     * Write everything that we can't parse to a gz file.
     * All data written to gz file is taken from httpResult, the Feed is taken to determine the path and
     * filename.
     */
    @Override
    public void onException(Feed feed, HttpResult httpResult) {

        long pollTimestamp = feed.getLastPollTime().getTime();
        boolean folderCreated = DatasetCreator.createDirectoriesAndCSV(feed);
        boolean gzWritten = false;
        if (folderCreated) {
            String folderPath = DatasetCreator.getFolderPath(feed.getId());
            gzWritten = writeGZ(httpResult, folderPath, pollTimestamp, "_unparsable");
        }
        if (!gzWritten) {
            throw new IllegalStateException("Error while writing GZ file");
        }

        processPollMetadata(feed, httpResult, null);
    }

    /**
     * Write a {@link HttpResult} to compressed file.
     * 
     * @param httpResult Result to write.
     * @param folderPath Path to write file to.
     * @param pollTimestamp The timestamp the data has been requested.
     * @param special Optional label to mark poll as unparsable, etc. Set to empty string if not required.
     * @return <code>true</code> if file has been written.
     */
    private boolean writeGZ(HttpResult httpResult, String folderPath, long pollTimestamp, String special) {
        String gzPath = folderPath + pollTimestamp + "_" + DateHelper.getDatetime("yyyy-MM-dd_HH-mm-ss", pollTimestamp)
                + special + ".gz";
        boolean gzWritten = HttpHelper.saveToFile(httpResult, gzPath, true);
        if (gzWritten) {
            LOGGER.debug("Saved " + special + " feed to: " + gzPath);
        } else {
            LOGGER.error("Could not save " + special + " feed to: " + gzPath);
        }
        return gzWritten;
    }

    /**
     * Put data to PollMetaInformation, write to database.
     * 
     * @param feed
     * @param httpResult
     * @param newItems
     */
    private void processPollMetadata(Feed feed, HttpResult httpResult, Integer newItems) {

        PollMetaInformation pollMetaInfo = new PollMetaInformation();

        pollMetaInfo.setFeedID(feed.getId());
        pollMetaInfo.setPollTimestamp(feed.getLastPollTime());
        pollMetaInfo.setHttpETag(httpResult.getHeaderString("ETag"));
        pollMetaInfo.setHttpDate(HttpHelper.getDateFromHeader(httpResult, "Date", true));
        pollMetaInfo.setHttpLastModified(HttpHelper.getDateFromHeader(httpResult, "Last-Modified", false));
        pollMetaInfo.setHttpExpires(HttpHelper.getDateFromHeader(httpResult, "Expires", false));
        pollMetaInfo.setNewestItemTimestamp(feed.getLastFeedEntry());
        pollMetaInfo.setNumberNewItems(newItems);
        pollMetaInfo.setWindowSize(feed.getWindowSize());
        pollMetaInfo.setHttpStatusCode(httpResult.getStatusCode());

        boolean success = feedStore.addFeedPoll(pollMetaInfo);
        if (!success) {
            throw new IllegalStateException("Error while adding feed poll");
        }
    }


    public static void main(String[] args) throws Exception {
        FeedParser fr = new RomeFeedParser();
        Feed feed = fr.getFeed("http://www.d3p.co.jp/rss/mobile.rdf");
        feed.setLastPollTime(new Date());
        FeedItem item = feed.getItems().get(0);
        System.out.println(buildCsvLine(item));
    }

}
