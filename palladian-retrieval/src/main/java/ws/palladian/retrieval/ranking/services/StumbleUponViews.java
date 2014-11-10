package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.json.JsonObject;
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
public final class StumbleUponViews extends AbstractRankingService implements RankingService {

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
        Ranking.Builder builder = new Ranking.Builder(this, url);
        if (isBlocked()) {
            return builder.create();
        }
        String requestUrl = "http://www.stumbleupon.com/services/1.01/badge.getinfo?url="
                + UrlHelper.encodeParameter(url);
        try {
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = httpResult.getStringContent();
            if (response != null) {
                JsonObject jsonObject = new JsonObject(response);
                JsonObject result = jsonObject.getJsonObject("result");
                Integer views = result.tryGetInt("views");
                builder.add(VIEWS, views != null ? views : 0);
                LOGGER.trace("Stumble Upon Views for " + url + " : " + views);
            }
        } catch (Exception e) {
            throw new RankingServiceException("url:" + url, e);
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
