package ws.palladian.retrieval.feeds.persistence;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import ws.palladian.retrieval.feeds.Feed;
import ws.palladian.retrieval.feeds.FeedItem;
import ws.palladian.retrieval.feeds.meta.PollMetaInformation;

/**
 * <p>
 * A feed source providing feeds from an in-memory collection. The collection is provided to an object of this class
 * upon its creation. Can be used as mock class for testing purposes when no database is available.
 * </p>
 * 
 * @author Klemens Muthmann
 * @author Philipp Katz
 * @author Sandro Reichert
 */
public class CollectionFeedSource implements FeedStore {

    /**
     * The collection of feeds this source provides.
     */
    private Collection<Feed> feeds;

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
        // mimic database behavior
        if (feed.getId() == -1) {
            feed.setId(feed.getFeedUrl().hashCode());
        }
        return feeds.add(feed);
    }

    @Override
    public boolean updateFeed(Feed feed) {
        return true;
    }

    @Override
    public List<Feed> getFeeds() {
        return new ArrayList<Feed>(feeds);
    }

    @Override
    public Feed getFeedByUrl(String feedUrl) {
        Feed ret = null;
        for (Feed feed : feeds) {
            if (feed.getFeedUrl().equals(feedUrl)) {
                ret = feed;
            }
        }
        return ret;
    }

    @Override
    public Feed getFeedById(int feedID) {
        Feed ret = null;
        for (Feed feed : feeds) {
            if (feed.getId() == feedID) {
                ret = feed;
            }
        }
        return ret;
    }

//    @Override
//    public boolean addFeedItem(FeedItem feedItem) {
//        // TODO Auto-generated method stub
//        return true;
//    }

//    @Override
//    public FeedItem getFeedItemByRawId(int feedId, String rawId) {
//        // TODO Auto-generated method stub
//        return null;
//    }

//    @Override
//    public List<FeedItem> getFeedItemsBySqlQuery(String sqlQuery) {
//        // TODO Auto-generated method stub
//        return null;
//    }

    @Override
    public int addFeedItems(List<FeedItem> items) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean updateMetaInformation(Feed feed) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean addFeedPoll(PollMetaInformation pollMetaInfo) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean updateFeed(Feed feed, boolean replaceCachedItems) {
        // nothing to do when feed is inMemory only and not written to data base.
        return true;
    }

//    @Override
//    public boolean deleteFeedByUrl(String feedUrl) {
//        boolean removed = false;
//        Collection<Feed> temp = new ArrayList<Feed>(feeds.size());
//        for (Feed feed : feeds) {
//            if (feed.getFeedUrl().equals(feedUrl)) {
//                removed = true;
//                continue;
//            }
//            temp.add(feed);
//        }
//        feeds = temp;
//        return removed;
//    }

}
