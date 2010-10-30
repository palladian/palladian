package tud.iir.news;

/**
 * Approach used for setting the interval a feed is checked for updates.
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @see FeedChecker
 * 
 */
public enum UpdateStrategy {

    /** Check each feed at a fixed interval. */
    UPDATE_FIXED,

    /** Check each feed and learn its update times. */
    UPDATE_ADAPTIVE,

    /** Check each feed and adapt to its update rate. */
    UPDATE_PROBABILISTIC;

}
