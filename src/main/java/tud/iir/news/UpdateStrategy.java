package tud.iir.news;

/**
 * Approach used for setting the interval a feed is checked for updates.
 * 
 * @author Klemens Muthmann
 * @author David Urbansky
 * @see FeedReader
 * 
 */
public enum UpdateStrategy {

    /** Check each feed at a fixed interval. */
    UPDATE_FIXED,

    /** Check each feed and learn its update times. */
    UPDATE_MOVING_AVERAGE,

    /** Check each feed and adapt to its update rate. */
    UPDATE_POST_RATE,

    /** Check with the best predictor for the feed. */
    UPDATE_POST_RATE_MOVING_AVERAGE;

}
