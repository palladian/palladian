package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.json.JsonArray;
import ws.palladian.retrieval.parser.json.JsonException;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * Get "Domain ranking based on Unique Visitor estimate for month/year" from Compete. Usage restriction: 1,000
 * requests/day.
 * </p>
 * 
 * @author Philipp Katz
 * @see http://developer.compete.com/
 */
public final class Compete extends BaseRankingService implements RankingService {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Compete.class);

    /** {@link Configuration} key for the API key. */
    public static final String CONFIG_API_KEY = "api.compete.key";

    /** The id of this service. */
    private static final String SERVICE_ID = "compete";

    /** The ranking value types of this service **/
    public static final RankingType UNIQUE_VISITORS = new RankingType("compete_unique_visitors",
            "Compete Unique Visitors", "");
    public static final RankingType VISITS = new RankingType("compete_visits", "Compete Visits", "");
    public static final RankingType RANK = new RankingType("compete_rank", "Compete Rank", "");

    /** All available ranking tpyes by Compete. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(UNIQUE_VISITORS, VISITS, RANK);

    private final String apiKey;

    /**
     * <p>
     * Create a new {@link Compete} ranking service.
     * </p>
     * 
     * @param configuration The configuration whch must provide an API key (<tt>api.compete.key</tt>) for accessing this
     *            service.
     */
    public Compete(Configuration configuration) {
        this(configuration.getString(CONFIG_API_KEY));
    }

    /**
     * <p>
     * Create a new {@link Compete} ranking service.
     * </p>
     * 
     * @param apiKey The required API key for accessing this service, not <code>null</code> or empty.
     */
    public Compete(String apiKey) {
        Validate.notEmpty(apiKey, "The required API key is missing.");
        this.apiKey = apiKey;
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {

        String domain = UrlHelper.getDomain(url, false);

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        results.put(UNIQUE_VISITORS, getMetrics(domain, "uv"));
        results.put(VISITS, getMetrics(domain, "vis"));
        results.put(RANK, getMetrics(domain, "rank"));

        Ranking ranking = new Ranking(this, url, results);
        return ranking;
    }

    private Float getMetrics(String domain, String metricCode) throws RankingServiceException {
        Float result = null;
        String requestUrl = "http://apps.compete.com/sites/" + domain + "/trended/" + metricCode + "/?apikey=" + apiKey
                + "&latest=1";
        try {
            HttpResult httpResult = retriever.httpGet(requestUrl);
            JsonObject jsonObject = new JsonObject(httpResult.getStringContent());
            String status = jsonObject.getString("status");
            if ("OK".equals(status)) {
                JsonArray metric = jsonObject.getJsonObject("data").getJsonObject("trends").getJsonArray(metricCode);
                result = (float) metric.getJsonObject(0).getInt("value");
                LOGGER.debug("metric=" + metricCode + " value=" + result);
            } else {
                LOGGER.warn("error: status = " + status);
            }
        } catch (HttpException e) {
            throw new RankingServiceException(e);
        } catch (JsonException e) {
            throw new RankingServiceException(e);
        }

        return result;
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
