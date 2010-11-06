/**
 * Created on: 23.07.2010 09:15:27
 */
package tud.iir.news.updates;

import java.util.List;

import tud.iir.news.Feed;
import tud.iir.news.FeedClassifier;
import tud.iir.news.FeedItem;
import tud.iir.news.FeedPostStatistics;

/**
 * <p>
 * 
 * </p>
 *
 * @author klemens.muthmann@googlemail.com
 * @version 1.0
 * @since 1.0
 *
 */
public class AdaptiveUpdateStrategy implements UpdateStrategy {

    /* (non-Javadoc)
     * @see tud.iir.news.UpdateStrategy#update(tud.iir.news.Feed, tud.iir.news.FeedPostStatistics)
     */
    @Override
    public void update(Feed feed, FeedPostStatistics fps) {

//        List<FeedEntry> entries = feed.getEntries();
//
//        // the factor by which the checkInterval is multiplied, ranges between 2 and 0.5
//        double f = 1.0;
//
//        // all news are new, we should halve the checkInterval
//        if (pnTarget > 1) {
//            f = 0.5;
//        }
//        // some entries are not new so we increase the checkInterval
//        else {
//            f = 2 - pnTarget;
//        }
//
//        int minCheckInterval = feed.getMinCheckInterval();
//        int maxCheckInterval = feed.getMaxCheckInterval();
//        maxCheckInterval *= f;
//
//        // for chunked or on the fly updates the min and max intervals are the same
//        if (feed.getUpdateClass() != FeedClassifier.CLASS_CHUNKED
//                && feed.getUpdateClass() != FeedClassifier.CLASS_ON_THE_FLY) {
//            minCheckInterval = maxCheckInterval / Math.max(1, entries.size() - 1);
//        } else {
//            minCheckInterval = maxCheckInterval;
//        }
//
//        feed.setMinCheckInterval(minCheckInterval);
//        feed.setMaxCheckInterval(maxCheckInterval);
//
//        // in case only one entry has been found use default check time
//        if (entries.size() == 1) {
//            feed.setMinCheckInterval(DEFAULT_CHECK_TIME / 2);
//            feed.setMaxCheckInterval(DEFAULT_CHECK_TIME);
//        }
    }

}
