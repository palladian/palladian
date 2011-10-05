package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

import ws.palladian.helper.ConfigHolder;
import ws.palladian.helper.nlp.StringHelper;
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
public class SharethisStats extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(SharethisStats.class);

    /** The config values. */
    private String apiKey;
    private String secret;

    /** The id of this service. */
    private static final String SERVICE_ID = "sharethis";

    /** The ranking value types of this service **/
    static RankingType SHARES = new RankingType("sharethis_stats", "ShareThis stats",
            "The number of shares via multiple services measured on sharethis.com.");
    static List<RankingType> RANKING_TYPES = new ArrayList<RankingType>();
    static {
        RANKING_TYPES.add(SHARES);
    }

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 60;

    public SharethisStats() {
        super();
        PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();

        if (configuration != null) {
            setApiKey(configuration.getString("api.sharethis.key"));
            setSecret(configuration.getString("api.sharethis.secret"));
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
            String encUrl = StringHelper.urlEncode(url);
            JSONObject json = retriever.getJSONDocument("http://rest.sharethis.com/reach/getUrlInfo.php?pub_key="
                    + getApiKey() + "&access_key=" + getSecret() + "&url=" + encUrl);
            if (json != null) {
                float total = json.getJSONObject("total").getInt("outbound");
                results.put(SHARES, total);
                LOGGER.trace("ShareThis stats for " + url + " : " + total);
            } else {
                results.put(SHARES, null);
                LOGGER.trace("ShareThis stats for " + url + "could not be fetched");
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
        boolean error = false;
        try {
            JSONObject json = retriever.getJSONDocument("http://rest.sharethis.com/reach/getUrlInfo.php?pub_key="
                    + getApiKey() + "&access_key=" + getSecret() + "&url=http://www.google.com/");
            if (json.has("statusMessage")) {
                if (json.get("statusMessage").equals("LIMIT_REACHED")) {
                    error = true;
                }
            }
        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
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

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setSecret(String secret) {
        this.secret = secret;
    }

    public String getSecret() {
        return secret;
    }

}
