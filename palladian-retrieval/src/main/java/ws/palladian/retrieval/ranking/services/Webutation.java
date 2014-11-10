package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.helper.UrlHelper;
import ws.palladian.retrieval.DocumentRetriever;
import ws.palladian.retrieval.parser.json.JsonObject;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * RankingService implementation to find the "Webutation" of a domain.
 * </p>
 * 
 * @author David Urbansky
 * @see http://www.webutation.net
 */
public final class Webutation extends AbstractRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = LoggerFactory.getLogger(Webutation.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "webutation";

    /** The ranking value types of this service **/
    public static final RankingType WEBUTATION = new RankingType("webutation", "Webutation",
            "The Webutation of a domain");

    /** All available ranking types by {@link Webutation}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(WEBUTATION);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Ranking.Builder builder = new Ranking.Builder(this, url);
        if (isBlocked()) {
            return builder.create();
        }

        double webutation = 0.;
        String requestUrl = buildRequestUrl(url);

        try {
            DocumentRetriever dretriever = new DocumentRetriever(retriever);
            JsonObject json = dretriever.getJsonObject(requestUrl);
            String value = json.tryQueryString("query/rating/value");

            if (!value.isEmpty()) {
                try {
                    webutation = Double.parseDouble(value) / 100;
                    LOGGER.trace("Webutation for " + url + " : " + webutation);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            throw new RankingServiceException(e.getMessage());
        }

        return builder.add(WEBUTATION, (float)webutation).create();
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
        return "http://api.webutation.net/?ver=1.1&type=json&q=" + UrlHelper.getDomain(url, false);
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
        Webutation gpl = new Webutation();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://palladian.ws");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(Webutation.WEBUTATION) + " trust");
    }

}
