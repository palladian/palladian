package ws.palladian.retrieval.ranking.services;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * RankingService implementation for clicks on a given url from Bit.ly.
 * </p>
 * <p>
 * Request limits (details unknown) are resetted every hour. Only 5 concurrent connections at the same time.
 * </p>
 * TODO: check all possible url variations and summarize?
 * TODO: limit threads to 5
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 * @see http://bit.ly/
 * @see http://code.google.com/p/bitly-api/wiki/ApiDocumentation
 * @see http://www.konkurrenzanalyse.net/reichenweite-auf-twitter-messen/
 */
public final class BitlyClicks extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BitlyClicks.class);

    /** {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.bitly.key";

    /** {@link Configuration} key for the login. */
    public static final String CONFIG_LOGIN = "api.bitly.login";

    /** The config values. */
    private final String apiKey;
    private final String login;

    /** The id of this service. */
    private static final String SERVICE_ID = "bitly";

    /** The ranking value types of this service **/
    public static final RankingType CLICKS = new RankingType("bitly_clicks", "Bit.ly Clicks",
            "The number of times users have clicked the shortened version of this url.");

    /** All available ranking types by {@link BitlyClicks}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(CLICKS);

    /** Fields to check the service availability. */
    private static boolean blocked = false;
    private static long lastCheckBlocked;
    private final static int checkBlockedIntervall = 1000 * 60 * 60;

    /**
     * <p>
     * Create a new {@link BitlyClicks} ranking service.
     * </p>
     * 
     * @param configuration The configuration which must provide a login (<tt>api.bitly.login</tt>) and an API key (
     *            <tt>api.bitly.key</tt>) for accessing this service.
     */
    public BitlyClicks(Configuration configuration) {
        this(configuration.getString(CONFIG_LOGIN), configuration.getString(CONFIG_API_KEY));
    }

    /**
     * <p>
     * Create a new {@link BitlyClicks} ranking service.
     * </p>
     * 
     * @param login The required login for accessing the service, not <code>null</code> or empty.
     * @param apiKey The required API key for accessing the service, not <code>null</code> or empty.
     */
    public BitlyClicks(String login, String apiKey) {
        Validate.notEmpty(login, "The required login is missing.");
        Validate.notEmpty(apiKey, "The required API key is missing.");
        this.login = login;
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

            // Step 1: get the bit.ly hash for the specified URL
            String hash = null;
            String encUrl = UrlHelper.encodeParameter(url);
            HttpResult httpResult = retriever.httpGet("http://api.bit.ly/v3/lookup?login=" + getLogin() + "&apiKey="
                    + getApiKey() + "&url=" + encUrl);
            JSONObject json = new JSONObject(HttpHelper.getStringContent(httpResult));
            if (checkJsonResponse(json)) {
                JSONObject lookup = json.getJSONObject("data").getJSONArray("lookup").getJSONObject(0);
                if (lookup.has("global_hash")) {
                    hash = lookup.getString("global_hash");
                    LOGGER.trace("Bit.ly hash for url " + url + " : " + hash);
                }

                // Step 2: get the # of clicks using the hash
                if (hash != null) {
                    HttpResult httpResult2 = retriever.httpGet("http://api.bit.ly/v3/clicks?login=" + getLogin()
                            + "&apiKey=" + getApiKey() + "&hash=" + hash);
                    json = new JSONObject(HttpHelper.getStringContent(httpResult2));
                    if (checkJsonResponse(json)) {
                        float result = json.getJSONObject("data").getJSONArray("clicks").getJSONObject(0)
                                .getInt("global_clicks");
                        results.put(CLICKS, result);
                        LOGGER.trace("Bit.ly clicks for " + url + " : " + result);
                    } else {
                        results.put(CLICKS, null);
                        LOGGER.trace("Bit.ly clicks for " + url + "could not be fetched");
                    }
                } else {
                    results.put(CLICKS, 0f);
                }
            } else {
                results.put(CLICKS, null);
                LOGGER.trace("Bit.ly clicks for " + url + "could not be fetched");
                checkBlocked();
            }

        } catch (JSONException e) {
            checkBlocked();
            throw new RankingServiceException(e);
        } catch (HttpException e) {
            checkBlocked();
            throw new RankingServiceException(e);
        }
        return ranking;
    }

    @Override
    public Map<String, Ranking> getRanking(List<String> urls) throws RankingServiceException {

        Map<String, Ranking> results = new HashMap<String, Ranking>();
        if (isBlocked()) {
            return results;
        }

        // iterate through urls in batches of 15, since this is the maximum number we
        // can send to bit.ly at once
        for (int index = 0; index < urls.size() / 15 + (urls.size() % 15 > 0 ? 1 : 0); index++) {

            List<String> subUrls = urls.subList(index * 15, Math.min(index * 15 + 15, urls.size()));
            String encUrls = "";
            String urlString = "";
            try {

                // Step 1: get the bit.ly hash for the specified URLs
                Map<String, String> hashes = new HashMap<String, String>();

                for (int i = 0; i < subUrls.size(); i++) {
                    encUrls += "&url=" + UrlHelper.encodeParameter(subUrls.get(i));
                }

                urlString = "http://api.bit.ly/v3/lookup?login=" + getLogin() + "&apiKey=" + getApiKey()
                        + "&mode=batch" + encUrls;

                HttpResult httpResult = retriever.httpGet(urlString);
                JSONObject json = new JSONObject(HttpHelper.getStringContent(httpResult));
                if (checkJsonResponse(json)) {
                    JSONArray lookups = json.getJSONObject("data").getJSONArray("lookup");
                    for (int i = 0; i < lookups.length(); i++) {
                        JSONObject lookup = lookups.getJSONObject(i);
                        if (lookup.has("global_hash")) {
                            hashes.put(lookup.getString("url"), lookup.getString("global_hash"));
                            LOGGER.trace("Bit.ly hash for url " + lookup.getString("url") + " : "
                                    + lookup.getString("global_hash"));
                        } else {
                            hashes.put(lookup.getString("url"), null);
                        }
                    }

                    // Step 2: get the # of clicks using the hash
                    String hashList = "";
                    for (String h : hashes.values()) {
                        if (h != null) {
                            hashList += "&hash=" + h;
                        }
                    }

                    Timestamp retrieved = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
                    if (hashList.length() > 0) {
                        urlString = "http://api.bit.ly/v3/clicks?login=" + getLogin() + "&apiKey=" + getApiKey()
                                + "&mode=batch" + hashList;
                        HttpResult httpResult2 = retriever.httpGet(urlString);
                        json = new JSONObject(HttpHelper.getStringContent(httpResult2));

                        if (checkJsonResponse(json)) {
                            JSONArray clicks = json.getJSONObject("data").getJSONArray("clicks");
                            float count = -1;
                            // iterate through all click results
                            for (int i = 0; i < clicks.length(); i++) {
                                JSONObject click = clicks.getJSONObject(i);
                                Map<RankingType, Float> result = new HashMap<RankingType, Float>();
                                if (click.has("global_clicks")) {
                                    count = click.getInt("global_clicks");
                                    result.put(CLICKS, count);
                                    // find url for the current hash and add ranking
                                    boolean found = false;
                                    String hash = click.getString("global_hash");
                                    Iterator<Entry<String, String>> it = hashes.entrySet().iterator();
                                    while (!found && it.hasNext()) {
                                        Entry<String, String> entry = it.next();
                                        if (hash.equals(entry.getValue())) {
                                            results.put(entry.getKey(), new Ranking(this, entry.getKey(), result,
                                                    retrieved));
                                            found = true;
                                            LOGGER.trace("Bit.ly clicks for hash " + click.getString("global_hash")
                                                    + " : " + click.getInt("global_clicks"));
                                        }
                                    }

                                }
                            }

                        }
                    }
                    for (String h : hashes.keySet()) {
                        if (hashes.get(h) == null) {
                            Map<RankingType, Float> result = new HashMap<RankingType, Float>();
                            result.put(CLICKS, 0f);
                            results.put(h, new Ranking(this, h, result, retrieved));
                        }
                    }
                } else {
                    for (String u : subUrls) {
                        Map<RankingType, Float> result = new HashMap<RankingType, Float>();
                        result.put(CLICKS, null);
                        results.put(u, new Ranking(this, u, result, new java.sql.Timestamp(Calendar.getInstance()
                                .getTime().getTime())));
                    }
                    LOGGER.trace("Bit.ly clicks for " + subUrls + " could not be fetched");
                    checkBlocked();
                }

            } catch (JSONException e) {
                checkBlocked();
                throw new RankingServiceException(e);
            } catch (HttpException e) {
                checkBlocked();
                throw new RankingServiceException(e);
            }

        }

        return results;

    }

    private boolean checkJsonResponse(JSONObject json) {
        if (json != null) {
            if (json.has("data")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            status = retriever.httpGet(
                    "http://api.bit.ly/v3/lookup?login=" + getLogin() + "&apiKey=" + getApiKey()
                            + "&url=http://www.google.com/").getStatusCode();
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
        LOGGER.error("Bit.ly Ranking Service is momentarily blocked. Will check again in 1h.");
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

    public String getLogin() {
        return login;
    }

}
