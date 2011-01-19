package tud.iir.news;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;

/**
 * Dummy/mock class which can be used instead of "real" database for testing purposes.
 * 
 * @author Philipp Katz
 * @deprecated functionality provided by {@link CollectionFeedSource}.
 */
@Deprecated
public class FeedStoreDummy implements FeedStore {

    private static final Logger logger = Logger.getLogger(FeedStoreDummy.class);

    @Override
    public boolean addFeedEntry(Feed feed, FeedItem entry) {
        logger.trace("adding " + entry);
        return true;
    }

    @Override
    public boolean addFeed(Feed feed) {
        logger.trace("adding " + feed);
        return true;
    }

    @Override
    public Feed getFeedByUrl(String feedUrl) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public List<Feed> getFeeds() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeedItem getFeedEntryByRawId(String rawId) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public boolean updateFeed(Feed feed) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public Feed getFeedByID(int feedID) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FeedItem getFeedEntryByRawId(int feedId, String rawId) {
        // TODO Auto-generated method stub
        return null;
    }

    public Set<Integer> getFeedEntryIdsTaggedAs(String tag) {
        logger.trace("getEntryIdsTaggedAs " + tag);
        return Collections.emptySet();
    }

    public List<FeedItem> getFeedEntries(String sqlQuery) {
        logger.trace("getFeedEntries " + sqlQuery);
        return Collections.emptyList();
    }

}
