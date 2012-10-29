package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find whether a certain URL has been indexed by Google, more specifically, whether it
 * is in the Cache of Google.
 * </p>
 * 
 * @author David Urbansky
 */
public final class GoogleIndexedPage extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(GoogleIndexedPage.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "googleindexed";

    /** The ranking value types of this service **/
    public static final RankingType GOOGLEINDEXED = new RankingType("googleindexed", "Google Indexed",
            "Whether the page is in Google's Cache");

    /** All available ranking types by {@link GoogleIndexedPage}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(GOOGLEINDEXED);

    @Override
    public Ranking getRanking(String url) {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        double indexed = 0.;
        String requestUrl = buildRequestUrl(url);

        HttpResult httpHead;
        try {
            httpHead = retriever.httpHead(requestUrl);

            if (httpHead.getStatusCode() != 404) {
                indexed = 1;
            }

        } catch (HttpException e) {
            LOGGER.error(e.getMessage());
        }

        results.put(GOOGLEINDEXED, (float)indexed);
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
        String requestUrl = "http://webcache.googleusercontent.com/search?q=cache:" + url;
        return requestUrl;
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

    public static void main(String[] a) {
        GoogleIndexedPage gpl = new GoogleIndexedPage();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://webknox.com/p/best-funny-comic-strips");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(GoogleIndexedPage.GOOGLEINDEXED) + " -> indexed");
    }

}
