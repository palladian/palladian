package ws.palladian.retrieval.search;

import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.date.DateHelper;
import ws.palladian.retrieval.resources.WebContent;

/**
 * <p>
 * Searcher decorator which waits, when a {@link RateLimitedException} happens. <b>Important</b>: For this to work, the
 * Searcher must throw dedicated {@link RateLimitedException}s, in case it is blocked because of rate limits.
 * </p>
 * 
 * @author pk
 * @param <R> Concrete type of search results.
 */
public final class RateLimitedWaitingSearcher<R extends WebContent> extends AbstractMultifacetSearcher<R> {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(RateLimitedWaitingSearcher.class);

    /** The default interval to wait, in case the searcher does not give an interval. */
    public static final int DEFAULT_WAIT_INTERVAL = (int)TimeUnit.MINUTES.toSeconds(1);

    /** The wrapped searcher. */
    private final Searcher<R> searcher;

    /**
     * <p>
     * Create a new {@link RateLimitedWaitingSearcher} with the specified {@link Searcher}.
     * </p>
     * 
     * @param searcher The searcher to wrap, not <code>null</code>.
     * @return A new {@link RateLimitedWaitingSearcher}.
     */
    public static <R extends WebContent> RateLimitedWaitingSearcher<R> create(Searcher<R> searcher) {
        Validate.notNull(searcher, "searcher must not be null");
        return new RateLimitedWaitingSearcher<R>(searcher);
    }

    private RateLimitedWaitingSearcher(Searcher<R> searcher) {
        this.searcher = searcher;
    }

    @Override
    public String getName() {
        return "RateLimitedWaitingSearcher for " + searcher.getName();
    }

    @Override
    public SearchResults<R> search(MultifacetQuery query) throws SearcherException {
        for (;;) {
            try {
                return searcher.search(query);
            } catch (RateLimitedException rle) {
                Integer timeUntilReset = rle.getTimeUntilReset();
                if (timeUntilReset == null) {
                    timeUntilReset = DEFAULT_WAIT_INTERVAL;
                } else {
                    // add some security buffer, just in case
                    timeUntilReset += 60;
                }
                long timeToSleep = timeUntilReset * 1000;
                LOGGER.info("Rate limit reached, waiting for {}", DateHelper.getTimeString(timeToSleep));
                try {
                    Thread.sleep(timeToSleep);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

}
