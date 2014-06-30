package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * RankingService implementation to find the number of backlinks to the domain using the OpenLinkProfiler index.
 * </p>
 * 
 * @author David Urbansky
 * 
 */
public final class OpenLinkProfiler extends AbstractRankingService implements RankingService {

    /** The id of this service. */
    private static final String SERVICE_ID = "openlinkprofiler";

    /** The ranking value types of this service **/
    public static final RankingType BACKLINKS_DOMAIN = new RankingType("openlinkprofilertotal", "Backlinks Total",
            "The Total Number of Backlinks to the Domain");
    public static final RankingType BACKLINKS_DOMAIN_UNIQUE = new RankingType("openlinkprofilerunique",
            "Unique Backlinks", "The Number of Unique Backlinks to the Domain");

    /** All available ranking types by {@link OpenLinkProfiler}. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(BACKLINKS_DOMAIN, BACKLINKS_DOMAIN_UNIQUE);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Map<RankingType, Float> results = new HashMap<RankingType, Float>();

        String requestUrl = buildRequestUrl(url);
        try {
            DocumentRetriever documentRetriever = new DocumentRetriever(retriever);
            Document document = documentRetriever.getWebDocument(requestUrl);

            Node node1 = XPathHelper.getXhtmlNode(document,
                    "//div/div[contains(@class,'topinfobox') and contains(@class,'help')]/p");
            long backlinksDomain = Long.valueOf(node1.getTextContent().replaceAll("[,+]", ""));
            long backlinksDomainUnique = Long.valueOf(XPathHelper
                    .getXhtmlNode(document, "//div/div[contains(@class,'topinfobox') and contains(@class,'2')][1]/p")
                    .getTextContent()
.replaceAll("[,+]", ""));

            results.put(BACKLINKS_DOMAIN, (float)backlinksDomain);
            results.put(BACKLINKS_DOMAIN_UNIQUE, (float)backlinksDomainUnique);

        } catch (Exception e) {
            throw new RankingServiceException("Error while parsing the response (\"" + url + "\")", e);
        }

        return new Ranking(this, url, results);
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
        return "http://www.openlinkprofiler.org/r/" + UrlHelper.getDomain(url, false);
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
        OpenLinkProfiler gpl = new OpenLinkProfiler();
        Ranking ranking = null;

        ranking = gpl.getRanking("http://webknox.com/");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(OpenLinkProfiler.BACKLINKS_DOMAIN) + " backlinks to the domain");
        System.out
        .println(ranking.getValues().get(OpenLinkProfiler.BACKLINKS_DOMAIN_UNIQUE) + " backlinks to the page");
    }

}
