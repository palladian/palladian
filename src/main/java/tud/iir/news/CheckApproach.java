/**
 * Created on: 22.07.2010 21:00:38
 */
package tud.iir.news;

/**
 * <p>
 * Approach used for setting the interval a feed is checked for updates.
 * </p>
 * 
 * @author klemens.muthmann@googlemail.com
 * @version 1.0
 * @since 1.0
 * @see FeedChecker
 * 
 */
public enum CheckApproach {
    /** check each feed at a fixed interval */
    CHECK_FIXED,
    /** check each feed and learn its update times */
    CHECK_ADAPTIVE,
    /** check each feed and adapt to its update rate */
    CHECK_PROBABILISTIC;
}
