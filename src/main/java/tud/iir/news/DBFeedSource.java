/**
 * Created on: 23.07.2010 08:23:06
 */
package tud.iir.news;

import java.util.Collection;

/**
 * <p>
 * A feed source that loads all feeds URLs from the toolkit database. Feed URLs are stored inside the 'feeds' table.
 * </p>
 *
 * @author klemens.muthmann@googlemail.com
 * @version 1.0
 * @since 1.0
 *
 */
public class DBFeedSource implements FeedSource {

    /* (non-Javadoc)
     * @see tud.iir.news.FeedSource#loadFeedList()
     */
    @Override
    public Collection<Feed> loadFeeds() {
        return FeedDatabase.getInstance().getFeeds();
    }

}
