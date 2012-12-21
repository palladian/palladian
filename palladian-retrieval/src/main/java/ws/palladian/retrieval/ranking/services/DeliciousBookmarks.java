package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
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
public final class DeliciousBookmarks extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(DeliciousBookmarks.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "delicious";

    /** The ranking value types of this service **/
    public static final RankingType BOOKMARKS = new RankingType("delicious_bookmarks", "Delicious Bookmarks",
            "The number of bookmarks users have created for this url.");

    /** All available ranking types by {@link DeliciousBookmarks}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(BOOKMARKS);

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

        Float result = null;

        try {

            String md5Url = DigestUtils.md5Hex(url);
            HttpResult httpResult = retriever.httpGet("http://feeds.delicious.com/v2/json/urlinfo/" + md5Url);
            String string = HttpHelper.getStringContent(httpResult);
            System.err.println(string);
            JSONArray json = new JSONArray(string);

            result = 0f;
            if (json.length() > 0) {
                result = (float) json.getJSONObject(0).getInt("total_posts");
            }
            LOGGER.trace("Delicious bookmarks for " + url + " : " + result);

        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
        }

        checkBlocked();
        results.put(BOOKMARKS, result);
        return ranking;
    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            status = retriever.httpGet(
                    "http://feeds.delicious.com/v2/json/urlinfo/" + DigestUtils.md5Hex("http://www.google.com/"))
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
