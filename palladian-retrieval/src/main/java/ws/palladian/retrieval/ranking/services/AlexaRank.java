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
import ws.palladian.retrieval.HttpException;
import ws.palladian.retrieval.HttpResult;
import ws.palladian.retrieval.parser.DocumentParser;
import ws.palladian.retrieval.parser.ParserException;
import ws.palladian.retrieval.parser.ParserFactory;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * Get Alexa popularity rank.
 * </p>
 * 
 * @author Philipp Katz
 * @see http://www.alexa.com/help/traffic-learn-more
 */
public final class AlexaRank extends BaseRankingService implements RankingService {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(AlexaRank.class);

    /** The id of this service. */
    private static final String SERVICE_ID = "alexa";

    /** The ranking value types of this service. */
    public static final RankingType POPULARITY_RANK = new RankingType("alexa_rank", "Alexa Rank", "");

    /** All available ranking types by AlexaRank. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(POPULARITY_RANK);

    @Override
    public Ranking getRanking(String url) {

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();

        try {
            String encUrl = UrlHelper.urlEncode(url);
            HttpResult httpResult = retriever.httpGet("http://data.alexa.com/data?cli=10&dat=s&url=" + encUrl);
            DocumentParser xmlParser = ParserFactory.createXmlParser();
            Document doc = xmlParser.parse(httpResult);

            Node popularityNode = XPathHelper.getNode(doc, "/ALEXA/SD/POPULARITY/@TEXT");
            if (popularityNode != null) {
                String popularity = popularityNode.getNodeValue();
                results.put(POPULARITY_RANK, Float.valueOf(popularity));
            } else {
                results.put(POPULARITY_RANK, 0f);
            }
        } catch (HttpException e) {
            LOGGER.error(e);
        } catch (ParserException e) {
            LOGGER.error(e);
        }

        Ranking ranking = new Ranking(this, url, results);
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
