package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
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
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
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
public class Compete extends BaseRankingService implements RankingService {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(Compete.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "compete";

    /** The ranking value types of this service **/
    static RankingType UNIQUE_VISITORS = new RankingType("compete_unique_visitors", "Compete Unique Visitors", "");
    static RankingType VISITS = new RankingType("compete_visits", "Compete Visits", "");
    static RankingType RANK = new RankingType("compete_rank", "Compete Rank", "");

    static List<RankingType> RANKING_TYPES = new ArrayList<RankingType>();
    static {
        RANKING_TYPES.add(UNIQUE_VISITORS);
        RANKING_TYPES.add(VISITS);
        RANKING_TYPES.add(RANK);
    }

    private String apiKey;

    public Compete() {
        super();
        PropertiesConfiguration configuration = ConfigHolder.getInstance().getConfig();

        if (configuration != null) {
            setApiKey(configuration.getString("api.compete.key"));
        } else {
            LOGGER.warn("could not load configuration, ranking retrieval won't work");
        }
    }

    @Override
    public Ranking getRanking(String url) {

        String domain = UrlHelper.getDomain(url, false);

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        results.put(UNIQUE_VISITORS, getMetrics(domain, "uv"));
        results.put(VISITS, getMetrics(domain, "vis"));
        results.put(RANK, getMetrics(domain, "rank"));

        Ranking ranking = new Ranking(this, url, results);
        return ranking;
    }

    private Float getMetrics(String domain, String metricCode) {
        Float result = null;
        String requestUrl = "http://apps.compete.com/sites/" + domain + "/trended/" + metricCode + "/?apikey=" + apiKey
                + "&latest=1";
        JSONObject jsonObject = retriever.getJSONDocument(requestUrl);

        if (jsonObject != null) {
            try {

                String status = jsonObject.getString("status");
                if ("OK".equals(status)) {
                    JSONArray metric = jsonObject.getJSONObject("data").getJSONObject("trends")
                            .getJSONArray(metricCode);
                    result = (float) metric.getJSONObject(0).getInt("value");
                    LOGGER.debug("metric=" + metricCode + " value=" + result);
                } else {
                    LOGGER.warn("error: status = " + status);
                }

            } catch (JSONException e) {
                LOGGER.error("JSONException", e);
            }
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

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getApiKey() {
        return apiKey;
    }

}
