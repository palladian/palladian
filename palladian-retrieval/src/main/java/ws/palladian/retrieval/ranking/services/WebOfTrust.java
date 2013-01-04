package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.UrlHelper;
import ws.palladian.helper.html.XPathHelper;
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * Get ranking from Web of Trust. We just take the "Trustworthiness" factor, not considering "Vendor reliability",
 * "Privacy", or "Child safety". Also we do not consider the confidence values.
 * </p>
 * 
 * @author Philipp Katz
 * @see http://www.mywot.com/en/api
 * @see http://www.mywot.com/wiki/API
 */
public final class WebOfTrust extends BaseRankingService implements RankingService {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(WebOfTrust.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "web_of_trust";

    /** The ranking value types of this service. */
    public static final RankingType TRUSTWORTHINESS = new RankingType("wot_trustworthiness",
            "Web of Trust Trustworthiness", "");

    /** All available ranking types by WebOfTrust. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(TRUSTWORTHINESS);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();
        Ranking ranking = new Ranking(this, url, results);

        String domain = UrlHelper.getDomain(url, false);
        try {
            HttpResult httpResult = retriever.httpGet("http://api.mywot.com/0.4/public_query2?target=" + domain);
            DocumentParser xmlParser = ParserFactory.createXmlParser();
            Document doc = xmlParser.parse(httpResult);

            Node trustworthiness = XPathHelper.getNode(doc, "//application[@name='0']/@r");
            if (trustworthiness != null) {
                Float trustValue = Float.valueOf(trustworthiness.getTextContent());
                LOGGER.trace("WOT Trustworthiness for " + url + " -> " + trustValue);
                results.put(TRUSTWORTHINESS, trustValue);
            }
        } catch (HttpException e) {
            throw new RankingServiceException("HttpException " + e.getMessage());
        } catch (ParserException e) {
            throw new RankingServiceException("ParserException " + e.getMessage());
        }

        return ranking;
    }

    @Override
    public String getServiceId() {
        return SERVICE_ID;
    }

    @Override
    public List<RankingType> getRankingTypes() {
        return RANKING_TYPES;
    }

}
