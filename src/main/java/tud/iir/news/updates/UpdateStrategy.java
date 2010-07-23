/**
 * Created on: 23.07.2010 09:07:24
 */
package tud.iir.news.updates;

import tud.iir.news.Feed;
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
     * 
     * </p>
     *
     * @param feed
     * @param fps
     */
    void update(Feed feed, FeedPostStatistics fps);
}
