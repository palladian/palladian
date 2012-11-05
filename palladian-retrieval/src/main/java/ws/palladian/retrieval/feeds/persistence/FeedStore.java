package ws.palladian.retrieval.feeds.persistence;

import java.util.List;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;

/**
 * <p>
 * The FeedStore is an interface for feed stores such as databases or file indices.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @author Sandro Reichert
 */
public interface FeedStore {

    /**
     * <p>
     * Add a new feed if its feedURL does not yet exist.
     * </p>
     * 
     * @param feed The feed to add.
     * @return true if feed was added successfully
     */
    boolean addFeed(Feed feed);

    /**
     * <p>
     * Update a feed if its feedURL already exists.
     * </p>
     * 
     * @param feed The feed to update.
     * @return True if feed was updated successfully.
     */
    boolean updateFeed(Feed feed);

    /**
     * <p>
     * Update a feed if its feedURL already exists.
     * </p>
     * 
     * @param feed The feed to update.
     * @param updateMetaInformation Specify whether the feed's meta information has to be updated or not.
     * @param replaceCachedItems If <code>true</code>, the cached items are replaced by the ones contained in the feed.
     * @return True if feed was updated successfully.
     */
    boolean updateFeed(Feed feed, boolean replaceCachedItems);

    /**
     * <p>
     * Remove a feed by its feedUrl
     * </p>
     * 
     * @param feedUrl
     * @return <code>true</code> if the feed was removed successfully, <code>false</code> otherwise.
     */
    boolean deleteFeedByUrl(String feedUrl);

    /**
     * <p>
     * Update the feed's meta information only.
     * </p>
     * 
     * @param feed The feed containing the meta information to update.
     * @return <code>true</code> if feed meta information was updated successfully.
     */
    boolean updateMetaInformation(Feed feed);

    /**
     * <p>
     * Get all feeds.
     * </p>
     * 
     * @return A list of all feeds from the store.
     */
    List<Feed> getFeeds();

    /**
     * <p>
     * Get a feed by its feedUrl.
     * </p>
     * 
     * @param feedUrl
     * @return The Feed with specified feedUrl, <code>null</code> if {@link Feed} does not exist.
     */
    Feed getFeedByUrl(String feedUrl);

    /**
     * <p>
     * If it does not yet exist, add a {@link FeedItem}.
     * </p>
     * 
     * @param item The {@link FeedItem} to add.
     */
    boolean addFeedItem(FeedItem item);

    /**
     * <p>
     * Add the not yet existing {@link FeedItem}s.
     * </p>
     * 
     * @param items The {@link FeedItem}s to add.
     * @return number of added {@link FeedItem}s.
     */
    int addFeedItems(List<FeedItem> items);

    /**
     * <p>
     * Get {@link FeedItem} for a specific feed by its rawId.
     * </p>
     * 
     * @param feedId
     * @param rawId
     * @return the {@link FeedItem} with specified rawId, <code>null</code> if {@link FeedItem} does not exist.
     */
    FeedItem getFeedItemByRawId(int feedId, String rawId);

    Feed getFeedById(int feedID);

    /**
     * <p>
     * Get FeedEntries by using a custom SQL query. The SELECT part must contain all appropriate columns with their
     * names from the feed_entries table.
     * </p>
     * 
     * @param sqlQuery
     * @return
     */
    List<FeedItem> getFeedItemsBySqlQuery(String sqlQuery);

    /**
     * Add information related to a poll.
     * 
     * @param pollMetaInfo The poll meta information to insert.
     * @return <code>true</code> if information was inserted, <code>false</code> otherwise.
     */
    boolean addFeedPoll(PollMetaInformation pollMetaInfo);

}