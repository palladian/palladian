package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation sharing statistics gathered from sharethis.com. Total value is counted, this includes
 * also services that may already be in Rankify! That's why commitment is 0.5.
 * </p>
 * <p>
 * Limit at 150 requests/hour, whitelisting possible.
 * </p>
 * TODO also use inbound value? (users that clicked on the shared link)
 * 
 * @author Julien Schmehl
 * @see http://www.sharethis.com/
 * @see http://help.sharethis.com/api/sharing-api#social-destinations
 */
public final class SharethisStats extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(SharethisStats.class);

    /** {@link Configuration} key for the secret. */
    public static final String CONFIG_SECRET = "api.sharethis.secret";

    /** {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.sharethis.key";

    /** The config values. */
    private final String apiKey;
    private final String secret;

    /** The id of this service. */
    private static final String SERVICE_ID = "sharethis";

    /** The ranking value types of this service **/
    public static final RankingType SHARES = new RankingType("sharethis_stats", "ShareThis stats",
            "The number of shares via multiple services measured on sharethis.com.");
    /** All available ranking types by {@link SharethisStats}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(SHARES);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 60;

    /**
     * <p>
     * Create a new {@link SharethisStats} ranking service.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key (<tt>api.sharethis.key</tt>) and a secret (
     *            <tt>api.sharethis.secret</tt>) for accessing the service.
     */
    public SharethisStats(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY), configuration.getString(CONFIG_SECRET));
    }

    /**
     * <p>
     * Create a new {@link SharethisStats} ranking service.
     * </p>
     * 
     * @param apiKey The required API key for accessing the service.
     * @param secret The required secret for accessing the service.
     */
    public SharethisStats(String apiKey, String secret) {
        super();
        if (apiKey == null || apiKey.isEmpty()) {
            throw new IllegalStateException("The required API key is missing");
        }
        if (secret == null || secret.isEmpty()) {
            throw new IllegalStateException("The required secret is missing.");
        }
        this.apiKey = apiKey;
        this.secret = secret;
    }

    @Override
    public Ranking getRanking(String url) {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        try {
            String encUrl = UrlHelper.urlEncode(url);
            HttpResult httpResult = retriever.httpGet("http://rest.sharethis.com/reach/getUrlInfo.php?pub_key="
                    + getApiKey() + "&access_key=" + getSecret() + "&url=" + encUrl);
            JSONObject json = new JSONObject(HttpHelper.getStringContent(httpResult));
            float total = json.getJSONObject("total").getInt("outbound");
            results.put(SHARES, total);
            LOGGER.trace("ShareThis stats for " + url + " : " + total);
        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
            checkBlocked();
        }
        return ranking;
    }

    @Override
    public boolean checkBlocked() {
        boolean error = false;
        try {
            HttpResult httpResult = retriever.httpGet("http://rest.sharethis.com/reach/getUrlInfo.php?pub_key="
                    + getApiKey() + "&access_key=" + getSecret() + "&url=http://www.google.com/");
            JSONObject json = new JSONObject(HttpHelper.getStringContent(httpResult));
            if (json.has("statusMessage")) {
                if (json.get("statusMessage").equals("LIMIT_REACHED")) {
                    error = true;
                }
            }
        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
        } catch (HttpException e) {
            LOGGER.error("HttpException " + e.getMessage());
        }
        if (!error) {
            blocked = false;
            lastCheckBlocked = new Date().getTime();
            return false;
        }
        blocked = true;
        lastCheckBlocked = new Date().getTime();
        LOGGER.error("ShareThis Ranking Service is momentarily blocked. Will check again in 1h. Try resetting your IP-Address.");
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

    public String getApiKey() {
        return apiKey;
    }

    public String getSecret() {
        return secret;
    }

}
