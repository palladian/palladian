package tud.iir.news.updates;

import tud.iir.news.Feed;
import tud.iir.news.FeedPostStatistics;

/**
 * <p>
 * 
 * </p>
 * 
 * @author David Urbansky
 * @author Klemens Muthmann
 * 
 */
public interface UpdateStrategy {

    /** Benchmark off. */
    public static final int BENCHMARK_OFF = 0;

    /** Benchmark algorithms towards their prediction ability for the next post. */
    public static final int BENCHMARK_MIN_CHECK_TIME = 1;

    /**
     * Benchmark algorithms towards their prediction ability for the next almost filled post list.
     */
    public static final int BENCHMARK_MAX_CHECK_TIME = 2;

    /**
     * <p>
     * Update the minimal and maximal update interval for the feed given the post statistics.
     * </p>
     * 
     * @param feed
     * @param fps
     */
    void update(Feed feed, FeedPostStatistics fps);

    public String getName();
}
