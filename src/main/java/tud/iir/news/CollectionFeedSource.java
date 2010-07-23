/**
 * Created on: 23.07.2010 08:31:26
 */
package tud.iir.news;

import java.util.Collection;

/**
 * <p>
 * A feed source providing feeds from a static collection. The collection is provided to an object of this class upon
 * its creation.
 * </p>
 * 
 * @author klemens.muthmann@googlemail.com
 * @version 1.0
 * @since 1.0
 * 
 */
public class CollectionFeedSource implements FeedSource {
    
    /**
     * <p>
     * The collection of feeds this source provides.
     * </p>
     */
    private final Collection<Feed> feeds;
    
    /**
     * <p>
     * Creates a new feed source for collections, initialized with an existing collection of feeds. 
     * </p>
     *
     * @param feeds The collection of feeds this source provides.
     */
    public CollectionFeedSource(final Collection<Feed> feeds) {
        if(feeds==null || feeds.isEmpty()) throw new IllegalArgumentException("Collection of feeds: "+feeds+" is not valid.");
        this.feeds = feeds;
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.news.FeedSource#loadFeedList()
     */
    @Override
    public Collection<Feed> loadFeeds() {
        return feeds;
    }

}
