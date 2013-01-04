package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.helper.HttpHelper;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to get the number of posts containing a given URL on plurk.com. Does fulltext search,
 * e.g. it finds also posts that have parts of the url - only usable for longer URLs.
 * </p>
 * <p>
 * Current limit is 50.000 calls pr. day
 * </p>
 * TODO implement follow up request if has_more:true
 * 
 * @author Julien Schmehl
 * @see http://www.plurk.com
 */
public final class PlurkPosts extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(PlurkPosts.class);

    /** {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.plurk.key";

    /** The config values. */
    private final String apiKey;

    /** The id of this service. */
    private static final String SERVICE_ID = "plurk";

    /** The ranking value types of this service **/
    public static final RankingType POSTS = new RankingType("plurk_posts", "Plurk.com posts",
            "The number of posts on plurk.com mentioning this url.");
    /** All available ranking types by {@link PlurkPosts}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(POSTS);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    /**
     * <p>
     * Create a new {@link PlurkPosts} ranking service.
     * </p>
     * 
     * @param configuration The configuration which must provide an API key (<tt>api.plurk.key</tt>) for accessing the
     *            service.
     */
    public PlurkPosts(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    /**
     * <p>
     * Create a new {@link PlurkPosts} ranking service.
     * </p>
     * 
     * @param apiKey The required API key for accessing the service, not <code>null</code> or empty.
     */
    public PlurkPosts(String apiKey) {
        Validate.notEmpty(apiKey, "The required API key is missing.");
        this.apiKey = apiKey;
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        try {

            String encUrl = UrlHelper.encodeParameter(url);
            HttpResult httpResult = retriever.httpGet("http://www.plurk.com/API/PlurkSearch/search?api_key="
                    + getApiKey() + "&query=" + encUrl);

            JSONObject json = new JSONObject(HttpHelper.getStringContent(httpResult));

            JSONArray plurks = json.getJSONArray("plurks");
            float result = plurks.length();
            results.put(POSTS, result);
            LOGGER.trace("Plurk.com posts for " + url + " : " + result);

        } catch (JSONException e) {
            checkBlocked();
            throw new RankingServiceException("JSONException " + e.getMessage(), e);
        } catch (HttpException e) {
            checkBlocked();
            throw new RankingServiceException("JSONException " + e.getMessage(), e);
        }
        return ranking;
    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            status = retriever.httpGet(
                    "http://www.plurk.com/API/PlurkSearch/search?api_key=" + getApiKey()
                            + "&query=http://www.google.com/").getStatusCode();
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
        LOGGER.error("Plurk Ranking Service is momentarily blocked. Will check again in 1min.");
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

}
