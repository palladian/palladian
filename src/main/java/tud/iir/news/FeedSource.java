/**
 * Created on: 23.07.2010 08:20:46
 */
package tud.iir.news;

import java.util.Collection;

/**
 * <p>
 * Describes a source for feed URLs. Implementing subclasses need to load these URLs via database, file, network or another external datastore.
 * </p>
 *
 * @author klemens.muthmann@googlemail.com
 * @version 1.0
 * @since 1.0
 *
 */
public interface FeedSource {
    /**
     * @return The list of all feeds from this feed source.
     */
    Collection<Feed> loadFeeds();
}
