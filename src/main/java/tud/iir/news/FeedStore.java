package tud.iir.news;

import java.util.List;

// TODO move to persistence
/**
 * The FeedStore is an interface for feed stores such as databases or file indices.
 * 
 * @author Philipp Katz
 * @author David Urbansky
 */
public interface FeedStore {

    /**
     * Add a new feed if its feedURL does not yet exist.
     * 
     * @param feed The feed to add.
     * @return true if feed was added successfully
     */
    boolean addFeed(Feed feed);

    /**
     * Update a feed if its feedURL already exists.
     * 
     * @param feed The feed to update.
     * @return True if feed was updated successfully.
     */
    boolean updateFeed(Feed feed);

    /**
     * Get all feeds.
     * 
     * @return A list of all feeds from the store.
     */
    List<Feed> getFeeds();

    /**
     * Get a feed by its feedUrl.
     * 
     * @param feedUrl
     * @return the Feed with specified feedUrl, <code>null</code> if Feed does not exist.
     */
    Feed getFeedByUrl(String feedUrl);

    /**
     * If it does not yet exist, add an entry to an existing feed.
     * 
     * @param feed
     * @param entry
     */
    boolean addFeedEntry(Feed feed, FeedItem entry);

    /**
     * Get an entry by its rawId.
     * 
     * @deprecated use {@link #getFeedEntryByRawId(int, String)} instead.
     * 
     * @param rawId
     * @return the FeedEntry with specified rawId, <code>null</code> if FeedEntry does not exist.
     */
    @Deprecated
    FeedItem getFeedEntryByRawId(String rawId);

    /**
     * Get an entry for a specific feed by its rawId.
     * 
     * @param feedId
     * @param rawId
     * @return the FeedEntry with specified rawId, <code>null</code> if FeedEntry does not exist.
     */
    FeedItem getFeedEntryByRawId(int feedId, String rawId);

    Feed getFeedByID(int feedID);

    /**
     * Get FeedEntries by using a custom SQL query. The SELECT part must contain all appropriate columns with their
     * names from the feed_entries table.
     * 
     * @param sqlQuery
     * @return
     */
    List<FeedItem> getFeedEntries(String sqlQuery);

    // Set<Integer> getFeedEntryIdsTaggedAs(String tag);

    // /**
    // * Get specified number of entries from a feed.
    // * @param feed
    // * @param limit specify the number of recent entries, -1 for no limit.
    // * @return
    // */
    // public abstract List<Entry> getEntries(Feed feed, int limit);
    //	
    // /**
    // * Get entries from a feed.
    // * @param feed
    // * @return
    // */
    // public abstract List<Entry> getEntries(Feed feed);

}