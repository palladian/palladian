/**
 * 
 */
package ws.palladian.web.ranking;

import java.util.Collections;
import java.util.Map;

import ws.palladian.helper.collection.TTLMap;
import ws.palladian.web.ranking.RankingRetriever.Service;

/**
 * In-memory cache for rankings using an expiring map.
 * 
 * @author Philipp Katz
 * 
 */
public class RankingCacheMemory extends RankingCache {

    private TTLMap<String, Map<Service, Float>> cache;

    public RankingCacheMemory() {
        cache = new TTLMap<String, Map<Service, Float>>();
        cache.setCleanInterval(1000);
        cache.setTimeToLive(getTtlSeconds() * 1000);
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.web.RankingCache#add(java.lang.String, java.util.Map)
     */
    @Override
    public void add(String url, Map<Service, Float> rankings) {
        cache.put(url, rankings);
    }

    /*
     * (non-Javadoc)
     * @see tud.iir.web.RankingCache#get(java.lang.String)
     */
    @Override
    public Map<Service, Float> get(String url) {
        Map<Service, Float> result = cache.get(url);
        if (result == null) {
            result = Collections.emptyMap();
        }
        return result;
    }

    @Override
    public void setTtlSeconds(int ttlSeconds) {
        super.setTtlSeconds(ttlSeconds);
        cache.setTimeToLive(ttlSeconds * 1000);
    }

}
