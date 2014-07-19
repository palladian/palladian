package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation for clicks on a given URL from <a href="http://bit.ly/">Bit.ly</a>. Request limits
 * (details unknown) are reset every hour. Only 5 concurrent connections at the same time.
 * </p>
 * 
 * @author Julien Schmehl
 * @author Philipp Katz
 * @see <a href="http://dev.bitly.com">Bitly API Documentation</a>
 */
public final class BitlyClicks extends AbstractRankingService {

    private static final int BATCH_SIZE = 15;

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
        return getRanking(Collections.singleton(url)).values().iterator().next();
    }

    @Override
    public Map<String, Ranking> getRanking(Collection<String> urls) throws RankingServiceException {
        Map<String, Ranking> results = new HashMap<String, Ranking>();
        if (isBlocked()) {
            return results;
        }
        // deduplicate provided URLs and create list
        List<String> urlList = CollectionHelper.newArrayList(CollectionHelper.newHashSet(urls));
        // iterate through urls in batches of 15, since this is the maximum number we can send to bit.ly at once
        int numRequests = (int)Math.ceil(urlList.size() / (float)BATCH_SIZE);
        for (int r = 0; r < numRequests; r++) {
            List<String> subUrls = urlList.subList(r * BATCH_SIZE,
                    Math.min(r * BATCH_SIZE + BATCH_SIZE, urlList.size()));
            try {
                // Step 1: get the bit.ly hash for the specified URLs
                Map<String, String> urlHashes = getBitlyHashes(subUrls);
                // Step 2: get the # of clicks using the hash
                Map<String, Integer> hashesClicks = getBitlyClicks(urlHashes.values());
                for (String url : subUrls) {
                    String hash = urlHashes.get(url);
                    Integer clicks = hashesClicks.get(hash);
                    if (clicks == null) { // "not found" on bitly
                        clicks = 0;
                    }
                    results.put(url, new Ranking.Builder(this, url).add(CLICKS, clicks).create());
                }
            } catch (JsonException e) {
                checkBlocked();
                throw new RankingServiceException(e);
            } catch (HttpException e) {
                checkBlocked();
                throw new RankingServiceException(e);
            }
        }
        return results;
    }

    /**
     * Retrieve the number of clicks for the given URL hashes.
     * 
     * @param bitlyHashes The URL hashes.
     * @return A map from URL hash to number of clicks.
     * @throws HttpException In case of HTTP errors.
     * @throws JsonException In case of parsing errors.
     */
    private Map<String, Integer> getBitlyClicks(Collection<String> bitlyHashes) throws HttpException, JsonException {
        StringBuilder hashes = new StringBuilder();
        for (String hash : bitlyHashes) {
            hashes.append("&hash=" + hash);
        }
        Map<String, Integer> clicks = CollectionHelper.newHashMap();
        if (hashes.length() > 0) {
            String url = "http://api.bit.ly/v3/clicks?login=" + login + "&apiKey=" + apiKey + "&mode=batch"
                    + hashes.toString();
            LOGGER.debug("clicks URL = {}", url);
            HttpResult httpResult = retriever.httpGet(url);
            LOGGER.debug("clicks JSON = {}", httpResult.getStringContent());
            JsonObject json = new JsonObject(httpResult.getStringContent());
            JsonArray clicksJson = json.getJsonObject("data").getJsonArray("clicks");
            // iterate through all click results
            for (int i = 0; i < clicksJson.size(); i++) {
                JsonObject click = clicksJson.getJsonObject(i);
                if (click.get("global_clicks") != null) {
                    int count = click.getInt("global_clicks");
                    // find url for the current hash and add ranking
                    String hash = click.getString("global_hash");
                    clicks.put(hash, count);
                }
            }
        }
        return clicks;
    }

    /**
     * Retrieve Bitly's URL hashes for the given URLs.
     * 
     * @param urls The URLs.
     * @return A map from full URL to short hash URL.
     * @throws HttpException In case of HTTP errors.
     * @throws JsonException In case of parsing errors.
     */
    private Map<String, String> getBitlyHashes(List<String> urls) throws HttpException, JsonException {
        Map<String, String> hashes = new HashMap<String, String>();
        StringBuilder encUrlsBuilder = new StringBuilder();
        for (int i = 0; i < urls.size(); i++) {
            encUrlsBuilder.append("&url=" + UrlHelper.encodeParameter(urls.get(i)));
        }
        String urlString = "http://api.bit.ly/v3/lookup?login=" + login + "&apiKey=" + apiKey + "&mode=batch"
                + encUrlsBuilder.toString();
        LOGGER.debug("lookup URL = {}", urlString);
        HttpResult httpResult = retriever.httpGet(urlString);
        LOGGER.debug("lookup JSON = {}", httpResult.getStringContent());
        JsonObject json = new JsonObject(httpResult.getStringContent());
        JsonArray lookups = json.getJsonObject("data").getJsonArray("lookup");
        for (int i = 0; i < lookups.size(); i++) {
            JsonObject lookup = lookups.getJsonObject(i);
            if (lookup.get("global_hash") != null) {
                hashes.put(lookup.getString("url"), lookup.getString("global_hash"));
                LOGGER.trace("Bit.ly hash for url " + lookup.getString("url") + " : " + lookup.getString("global_hash"));
            }
        }
        return hashes;
    }

    @Override
    public boolean checkBlocked() {
        int status = -1;
        try {
            status = retriever.httpGet(
                    "http://api.bit.ly/v3/lookup?login=" + login + "&apiKey=" + apiKey + "&url=http://www.google.com/")
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

}
