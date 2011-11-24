package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
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
public class PlurkPosts extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(PlurkPosts.class);

    /** The config values. */
    private String apiKey;

    /** The id of this service. */
    private static final String SERVICE_ID = "plurk";

    /** The ranking value types of this service **/
    public static final RankingType POSTS = new RankingType("plurk_posts", "Plurk.com posts",
            "The number of posts on plurk.com mentioning this url.");
    private static final List<RankingType> RANKING_TYPES = new ArrayList<RankingType>();
    static {
        RANKING_TYPES.add(POSTS);
    }

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    public PlurkPosts() {
        super();

        PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();

        if (configuration != null) {
            setApiKey(configuration.getString("api.plurk.key"));
        } else {
            LOGGER.warn("could not load configuration, ranking retrieval won't work");
        }
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
            JSONObject json = retriever.getJSONDocument("http://www.plurk.com/API/PlurkSearch/search?api_key="
                    + getApiKey() + "&query=" + encUrl);

            if (json != null) {
                JSONArray plurks = json.getJSONArray("plurks");
                float result = plurks.length();
                results.put(POSTS, result);
                LOGGER.trace("Plurk.com posts for " + url + " : " + result);
            } else {
                results.put(POSTS, null);
                LOGGER.trace("Plurk.com posts for " + url + "could not be fetched");
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

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

}
