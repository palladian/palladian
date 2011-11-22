package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;

import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to get the number of bookmarks of a given url on Delicious.
 * </p>
 * <p>
 * Wait at least 1 second between requests. Feeds only updated 1-2 times per hour.
 * </p>
 * TODO use proxies to overcome limits
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 * @see http://delicious.com/
 * @see http://delicious.com/help/feeds
 */
public class DeliciousBookmarks extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(DeliciousBookmarks.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "delicious";

    /** The ranking value types of this service **/
    public static final RankingType BOOKMARKS = new RankingType("delicious_bookmarks", "Delicious Bookmarks",
            "The number of bookmarks users have created for this url.");

    private static final List<RankingType> RANKING_TYPES = new ArrayList<RankingType>();
    static {
        RANKING_TYPES.add(BOOKMARKS);
    }

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    public DeliciousBookmarks() {

        super();

        // using different proxies for each request to avoid 1 second request limit
        // doesn't work with every proxy
        // crawler.setSwitchProxyRequests(1);
        // crawler.setProxyList(configuration.getList("documentRetriever.proxyList"));

    }

    @Override
    public Ranking getRanking(String url) {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        try {

            // String encUrl = StringHelper.urlEncode(url);
            // JSONArray json = retriever.getJSONArray("http://feeds.delicious.com/v2/json/urlinfo?url=" + encUrl);
            
            String md5Url = DigestUtils.md5Hex(url);
            JSONArray json = retriever.getJSONArray("http://feeds.delicious.com/v2/json/urlinfo/" + md5Url);

            if (json != null) {
                float result = 0;
                if (json.length() > 0) {
                    result = json.getJSONObject(0).getInt("total_posts");
                }
                results.put(BOOKMARKS, result);
                LOGGER.trace("Delicious bookmarks for " + url + " : " + result);
            } else {
                results.put(BOOKMARKS, null);
                LOGGER.trace("Delicious bookmarks for " + url + " could not be fetched");
                checkBlocked();
            }

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        }
        return ranking;
    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            status = retriever.httpGet("http://feeds.delicious.com/v2/json/urlinfo?url=http://www.google.com/")
                    .getStatusCode();
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
        }
        if (status == 200) {
            blocked = false;
            lastCheckBlocked = new Date().getTime();
            return false;
        }
        blocked = true;
        lastCheckBlocked = new Date().getTime();
        LOGGER.error("Delicious Ranking Service is momentarily blocked. Will check again in 1min. Try changing your IP-Address.");
        return true;
    }

    @Override
    public boolean isBlocked() {
        if (new Date().getTime() - lastCheckBlocked < checkBlockedIntervall) {
            return blocked;
        } else {
            return checkBlocked();
        }
    }

    @Override
    public void resetBlocked() {
        blocked = false;
        lastCheckBlocked = new Date().getTime();
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }
}
