package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * RankingService implementation to find the number of pins on Pinterest of a Web page.
 * </p>
 * <p>
 * <b>NOTE: Pinterest has no official API yet so we use sharedcount instead.</b>
 * </p>
 * 
 * TODO sharedcount also offers a few more interesting ranking types
 * 
 * 
 * @author David Urbansky
 * 
 */
public final class PinterestPins extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(PinterestPins.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "pinterest";

    /** The ranking value types of this service **/
    public static final RankingType PINS = new RankingType("pinterestpins", "Pinterest Pins",
            "The Number of Pins on Pinterest");

    /** All available ranking types by {@link PinterestPins}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(PINS);


    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        Integer pins = 0;
        String requestUrl = buildRequestUrl(url);

        try {
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = httpResult.getStringContent();

            if (response != null) {
                JsonObject jsonObject = new JsonObject(response);

                pins = jsonObject.getInt("Pinterest");

                LOGGER.trace("Pinterest Pins for " + url + " : " + pins);
            }
        } catch (Exception e) {
            throw new RankingServiceException(e);
        }

        results.put(PINS, (float)pins);
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
        return "http://api.sharedcount.com/?url=" + UrlHelper.encodeParameter(url);
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
        PinterestPins gpl = new PinterestPins();
        Ranking ranking = null;

        ranking = gpl
                .getRanking("http://www.g4tv.com/attackoftheshow/blog/post/712294/punishing-bad-parking-jobs/");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(PinterestPins.PINS) + " pins");
    }

}
