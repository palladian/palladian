package ws.palladian.retrieval.ranking.services;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.persistence.json.JsonException;
import ws.palladian.persistence.json.JsonObject;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

import java.util.Arrays;
import java.util.List;

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
public final class SharethisStats extends AbstractRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SharethisStats.class);

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
    public static final RankingType SHARES = new RankingType("sharethis_stats", "ShareThis stats", "The number of shares via multiple services measured on sharethis.com.");
    /** All available ranking types by {@link SharethisStats}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(SHARES);

    /**
     * <p>
     * Create a new {@link SharethisStats} ranking service.
     * </p>
     *
     * @param configuration The configuration which must provide an API key (<tt>api.sharethis.key</tt>) and a secret (
     *                      <tt>api.sharethis.secret</tt>) for accessing the service.
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
        Validate.notEmpty(apiKey, "The required API key is missing");
        Validate.notEmpty(secret, "The required secret is missing.");
        this.apiKey = apiKey;
        this.secret = secret;
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);

        try {
            String encUrl = UrlHelper.encodeParameter(url);
            HttpResult httpResult = retriever.httpGet("http://rest.sharethis.com/reach/getUrlInfo.php?pub_key=" + getApiKey() + "&access_key=" + getSecret() + "&url=" + encUrl);
            JsonObject json = new JsonObject(httpResult.getStringContent());
            int total = json.getJsonObject("total").getInt("outbound");
            builder.add(SHARES, total);
            LOGGER.trace("ShareThis stats for " + url + " : " + total);
        } catch (JsonException e) {
            throw new RankingServiceException("JSONException " + e.getMessage(), e);
        } catch (HttpException e) {
            throw new RankingServiceException("JSONException " + e.getMessage(), e);
        }
        return builder.create();
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
