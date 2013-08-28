package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find the number of views of a Web page over Stumble Upon.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public final class StumbleUponViews extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(StumbleUponViews.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "stumbleupon";

    /** The ranking value types of this service **/
    public static final RankingType VIEWS = new RankingType("stumbleuponviews", "Stumble Upon Views",
            "The Number of Views on Stumble Upon");

    /** All available ranking types by {@link StumbleUponViews}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(VIEWS);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        Integer views = 0;
        String requestUrl = buildRequestUrl(url);

        try {
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = httpResult.getStringContent();

            if (response != null) {
                JSONObject jsonObject = new JSONObject(response);

                JSONObject result = jsonObject.getJSONObject("result");
                views = result.getInt("views");

                LOGGER.trace("Stumble Upon Views for " + url + " : " + views);
            }
        } catch (Exception e) {
            throw new RankingServiceException("url:" + url, e);
        }

        results.put(VIEWS, (float)views);
        return ranking;
    }

    /**
     * <p>
     * Build the request URL.
     * </p>
     * 
     * @param url The URL to search for.
     * @return The request URL.
     */
    private String buildRequestUrl(String url) {
        return "http://www.stumbleupon.com/services/1.01/badge.getinfo?url=" + UrlHelper.encodeParameter(url);
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] a) throws RankingServiceException {
        StumbleUponViews gpl = new StumbleUponViews();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://webknox.com");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(StumbleUponViews.VIEWS) + " views");

        ranking = gpl.getRanking("http://palladian.ws");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(StumbleUponViews.VIEWS) + " views");
    }

}
