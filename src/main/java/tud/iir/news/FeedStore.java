package tud.iir.news;

import java.util.List;

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
    public abstract boolean addFeed(Feed feed);

    /**
     * Update a feed if its feedURL already exists.
     * 
     * @param feed The feed to update.
     * @return true if feed was updated successfully
     */
    public abstract boolean updateFeed(Feed feed);

    /**
     * Get all feeds.
     * 
     * @return A list of all feeds from the store.
     */
    public abstract List<Feed> getFeeds();

    /**
     * Get a feed by its feedUrl.
     * 
     * @param feedUrl
     * @return
     */
    public abstract Feed getFeedByUrl(String feedUrl);

    /**
     * If it does not yet exist, add an entry to an existing feed.
     * 
     * @param feed
     * @param entry
     */
    public abstract boolean addEntry(Feed feed, FeedEntry entry);

    /**
     * Get an entry by its rawId.
     * 
     * @return
     */
    public abstract FeedEntry getEntryByRawId(String rawId);

    Feed getFeedByID(int feedID);

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