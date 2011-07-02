package ws.palladian.retrieval.feeds.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;

/**
 * A feed source providing feeds from a static collection. The collection is provided to an object of this class upon
 * its creation. Can be used as mock class for testing purposes when no database is available.
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @author Sandro Reichert
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
        if (feeds == null) {
            throw new NullPointerException("Collection of feeds must not be null.");
        }
        this.feeds = feeds;
    }

    /**
     * Creates a new feed source initialized with an empty collection.
     */
    public CollectionFeedSource() {
        this(new ArrayList<Feed>());
    }

    @Override
    public boolean addFeed(Feed feed) {
        return feeds.add(feed);
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
    public boolean addFeedItem(Feed feed, FeedItem entry) {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public FeedItem getFeedItemByRawId(int feedId, String rawId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<FeedItem> getFeedItemsBySqlQuery(String sqlQuery) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public int addFeedItems(Feed feed, List<FeedItem> items) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean updateMetaInformation(Feed feed) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateFeed(Feed feed, boolean updateMetaInformation) {
        // TODO Auto-generated method stub
        return false;
    }

}
