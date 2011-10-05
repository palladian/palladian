package ws.palladian.retrieval.ranking.services;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.PropertiesConfiguration;
import org.apache.log4j.Logger;
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
 * RankingService implementation for shares of a given url on Google Buzz.
 * </p>
 * <p>
 * Courtesy limit: 1,000,000 requests/day & 100,000 requests/second/user.
 * </p>
 * 
 * @author Julien Schmehl
 * @see http://www.google.com/buzz
 */
public class GoogleBuzzShares extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(GoogleBuzzShares.class);

    /** The config values. */
    private String apiKey;

    /** The id of this service. */
    private static final String SERVICE_ID = "buzz";

    /** The ranking value types of this service **/
    static RankingType SHARES = new RankingType("buzz_shares", "Google Buzz Shares",
            "The number of times users have shared the page on Google Buzz");
    static List<RankingType> RANKING_TYPES = new ArrayList<RankingType>();
    static {
        RANKING_TYPES.add(SHARES);
    }

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 1;

    public GoogleBuzzShares() {

        super();

        PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();

        if (configuration != null) {
            setApiKey(configuration.getString("api.google.buzz.key"));
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
            JSONObject json = retriever.getJSONDocument("https://www.googleapis.com/buzz/v1/activities/count?key="
                    + getApiKey() + "&alt=json&url=" + encUrl);
            if (json != null) {
                float result = json.getJSONObject("data").getJSONObject("counts").getJSONArray(url).getJSONObject(0)
                        .getInt("count");
                results.put(SHARES, result);
                LOGGER.trace("Google Buzz shares for " + url + " : " + result);
            } else {
                results.put(SHARES, null);
                LOGGER.trace("Google Buzz shares for " + url + "could not be fetched");
                checkBlocked();
            }
        } catch (JSONException e) {
            LOGGER.error("JSONException " + e.getMessage());
            checkBlocked();
        }
        return ranking;
    }

    @Override
    public Map<String, Ranking> getRanking(List<String> urls) {
        Map<String, Ranking> results = new HashMap<String, Ranking>();
        if (isBlocked()) {
            return results;
        }

        // iterate through urls in batches of 10, since this is the maximum number we
        // can send to bit.ly at once
        for (int index = 0; index < urls.size() / 10 + (urls.size() % 10 > 0 ? 1 : 0); index++) {

            List<String> subUrls = urls.subList(index * 10, Math.min(index * 10 + 10, urls.size()));
            String encUrls = "";

            try {
                for (int i = 0; i < subUrls.size(); i++) {
                    encUrls += "&url=" + UrlHelper.urlEncode(subUrls.get(i));
                }

                JSONObject json = retriever.getJSONDocument("https://www.googleapis.com/buzz/v1/activities/count?key="
                        + getApiKey() + "&alt=json" + encUrls);
                Timestamp retrieved = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
                if (json != null) {

                    float count = -1;

                    for (String u : subUrls) {
                        count = json.getJSONObject("data").getJSONObject("counts").getJSONArray(u).getJSONObject(0)
                                .getInt("count");
                        Map<RankingType, Float> result = new HashMap<RankingType, Float>();
                        result.put(SHARES, count);
                        results.put(u, new Ranking(this, u, result, retrieved));
                        LOGGER.trace("Google Buzz shares for " + u + " : " + count);
                    }
                } else {
                    for (String u : subUrls) {
                        Map<RankingType, Float> result = new HashMap<RankingType, Float>();
                        result.put(SHARES, null);
                        results.put(u, new Ranking(this, u, result, retrieved));
                    }
                    LOGGER.trace("Google Buzz shares for " + subUrls + "could not be fetched");
                    checkBlocked();
                }
            } catch (JSONException e) {
                LOGGER.error("JSONException " + e.getMessage());
                checkBlocked();
            }
        }
        return results;

    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            status = retriever.httpGet(
                    "https://www.googleapis.com/buzz/v1/activities/count?key=" + getApiKey()
                            + "&alt=json&url=http://www.google.com/").getStatusCode();
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
        LOGGER.error("Google Buzz Ranking Service is momentarily blocked. Will check again in 1min. Try changing your IP-address.");
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
