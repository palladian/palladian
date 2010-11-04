package tud.iir.news;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * A feed source providing feeds from a static collection. The collection is provided to an object of this class upon
 * its creation.
 * 
 * @author Klemens Muthmann
 * 
 */
public class CollectionFeedSource implements FeedStore {

    /**
     * The collection of feeds this source provides.
     */
    private final Collection<Feed> feeds;

    /**
     * Creates a new feed source for collections, initialized with an existing collection of feeds.
     * 
     * @param feeds The collection of feeds this source provides.
     */
    public CollectionFeedSource(final Collection<Feed> feeds) {
        if (feeds == null || feeds.isEmpty()) {
            throw new IllegalArgumentException("Collection of feeds: " + feeds + " is not valid.");
        }
        this.feeds = feeds;
    }

    @Override
    public boolean addFeed(Feed feed) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateFeed(Feed feed) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public List<Feed> getFeeds() {
        List<Feed> ret = new ArrayList<Feed>(feeds.size());
        ret.addAll(feeds);
        return ret;
    }

    @Override
    public Feed getFeedByUrl(String feedUrl) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Feed getFeedByID(int feedID) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean addFeedEntry(Feed feed, FeedEntry entry) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public FeedEntry getFeedEntryByRawId(String rawId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeedEntry getFeedEntryByRawId(int feedId, String rawId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<FeedEntry> getFeedEntries(String sqlQuery) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Set<Integer> getFeedEntryIdsTaggedAs(String tag) {
        // TODO Auto-generated method stub
        return null;
    }

}
