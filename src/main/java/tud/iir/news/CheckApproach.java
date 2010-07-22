package tud.iir.news;

/**
 * Approach used for setting the interval a feed is checked for updates.
 * 
 * @author Klemens Muthmann
 * @see FeedChecker
 * 
 */
public enum CheckApproach {

    /** Check each feed at a fixed interval. */
    CHECK_FIXED,

    /** Check each feed and learn its update times. */
    CHECK_ADAPTIVE,

    /** Check each feed and adapt to its update rate. */
    CHECK_PROBABILISTIC;

}
