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
 * RankingService implementation to find whether a certain URL has been accepted to DMOZ.
 * </p>
 * 
 * @author David Urbansky
 */
public final class DmozIndexed extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(DmozIndexed.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "DMOZ";

    /** The ranking value types of this service **/
    public static final RankingType DMOZ_INDEXED = new RankingType(SERVICE_ID, "DMOZ Accepted",
            "Whether the page has been accepted to DMOZ.");

    /** All available ranking types by {@link DmozIndexed}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(DMOZ_INDEXED);

    @Override
    public Ranking getRanking(String url) {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);

        double indexed = 0.;
        String requestUrl = buildRequestUrl(url);

        HttpResult httpGet;
        try {

            httpGet = retriever.httpGet(requestUrl);
            String content = new String(httpGet.getContent());

            if (content.contains("small>(1-")) {
                indexed = 1;
            }

        } catch (HttpException e) {
            LOGGER.error(e.getMessage());
        }

        results.put(DMOZ_INDEXED, (float)indexed);
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
        String requestUrl = "http://www.dmoz.org/search?q=" + url;
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
        DmozIndexed gpl = new DmozIndexed();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://google.com");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(DmozIndexed.DMOZ_INDEXED) + " -> in DMOZ");
    }

}
