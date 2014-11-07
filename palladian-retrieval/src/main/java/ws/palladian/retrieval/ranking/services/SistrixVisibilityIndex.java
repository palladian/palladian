package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.nlp.StringHelper;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find the visibility index for a given domain.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public final class SistrixVisibilityIndex extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(SistrixVisibilityIndex.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "sistrix";

    /** The ranking value types of this service **/
    public static final RankingType INDEX = new RankingType("index", "Sistrix Visibility Index",
            "The global visibility index of the domain according to Sistrix.");

    /** All available ranking types by {@link SistrixVisibilityIndex}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(INDEX);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        url = UrlHelper.getDomain(url, false);

        Double index = 0.;
        String requestUrl = buildRequestUrl(url);

        try {
            HttpResult httpResult = retriever.httpGet(requestUrl);
            String response = httpResult.getStringContent();

            if (response != null) {
                index = Double.valueOf(StringHelper.getSubstringBetween(response, "<h3>", "</h3>").replace(".", "")
                        .replace(",", "."));
                LOGGER.trace("Sistrix Visibility Index for " + url + " : " + index);
            }
        } catch (Exception e) {
            throw new RankingServiceException("url:" + url, e);
        }

        results.put(INDEX, index.floatValue());
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
        return "http://www.sichtbarkeitsindex.de/" + UrlHelper.encodeParameter(url);
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
        SistrixVisibilityIndex gpl = new SistrixVisibilityIndex();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://wikipedia.org");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(SistrixVisibilityIndex.INDEX));
    }

}
