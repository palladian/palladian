/**
 * 
 */
package tud.iir.web.ranking;

import java.util.Collections;
import java.util.Map;

import tud.iir.web.ranking.RankingRetriever.Service;

/**
 * TODO implement a real in-memory cache, using an expiring map.
 * http://www.vipan.com/htdocs/cachehelp.html
 * @author Philipp Katz
 *
 */
public class RankingCacheMemory extends RankingCache {

    /* (non-Javadoc)
     * @see tud.iir.web.URLRankingCache#add(java.lang.String, java.util.Map)
     */
    @Override
    public void add(String url, Map<Service, Float> rankings) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see tud.iir.web.URLRankingCache#get(java.lang.String)
     */
    @Override
    public Map<Service, Float> get(String url) {
        return Collections.emptyMap();
    }

}
