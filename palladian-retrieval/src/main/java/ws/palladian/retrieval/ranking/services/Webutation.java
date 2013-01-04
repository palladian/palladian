package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.DocumentRetriever;
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
public final class Webutation extends BaseRankingService implements RankingService {

    /** The class logger. */
    private static final Logger LOGGER = Logger.getLogger(Webutation.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "webutation";

    /** The ranking value types of this service **/
    public static final RankingType WEBUTATION = new RankingType("webutation", "Webutation",
            "The Webutation of a domain");

    /** All available ranking types by {@link Webutation}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(WEBUTATION);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);
        if (isBlocked()) {
            return ranking;
        }

        double webutation = 0.;
        String requestUrl = buildRequestUrl(url);

        try {
            DocumentRetriever dretriever = new DocumentRetriever(retriever);
            Document document = dretriever.getWebDocument(requestUrl);
            Node scoreNode = XPathHelper.getXhtmlNode(document, "//div[@id='badge']//span");

            if (scoreNode != null) {
                try {
                    webutation = Integer.valueOf(scoreNode.getTextContent()) / 100;
                    LOGGER.trace("Webutation for " + url + " : " + webutation);
                } catch (Exception e) {
                }
            }
        } catch (Exception e) {
            throw new RankingServiceException(e.getMessage());
        }

        results.put(WEBUTATION, (float)webutation);
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
        return "http://www.webutation.net/go/review/" + UrlHelper.getDomain(url, false);
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
