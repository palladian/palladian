package ws.palladian.web.ranking;

import java.util.Map;

import ws.palladian.web.ranking.RankingRetriever.Service;

/**
 * Abstract super class for caching implementations.
 * 
 * @author Philipp Katz
 *
 */
public abstract class RankingCache {

    /** Default time interval in seconds, after the cached values are considered invalid is set to 24 hours. */
    private static final int DEFAULT_TTL_SECONDS = 60 * 60 * 24;

    private int ttlSeconds = DEFAULT_TTL_SECONDS;

    /**
     * Get cached ranking values for specified URL. Returns only those values which are under the specified TTL or
     * an empty list if there are no cached or up-to-date ranking values, never <code>null</code>.
     * 
     * @param url
     * @return
     */
    abstract Map<Service, Float> get(String url);

    /**
     * Adds or updates rankings for a specific URL in the cache.
     * 
     * @param url
     * @param rankings A Map with all cached rankings for the specified url.
     */
    abstract void add(String url, Map<Service, Float> rankings);

    /**
     * Set the TTL for the cache. Set to -1 to never expire the cached data.
     * 
     * @param ttlSeconds
     */
    public void setTtlSeconds(int ttlSeconds) {
        this.ttlSeconds = ttlSeconds;
    }

    /**
     * Get the TTL for the cache.
     * 
     * @return
     */
    public int getTtlSeconds() {
        return ttlSeconds;
    }

}