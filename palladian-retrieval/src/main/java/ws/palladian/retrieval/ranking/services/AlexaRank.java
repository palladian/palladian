package ws.palladian.retrieval.ranking.services;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * Get Alexa popularity rank.
 * </p>
 * 
 * @author Philipp Katz
 * @author David Urbansky
 * @see http://www.alexa.com/help/traffic-learn-more
 */
public final class AlexaRank extends AbstractRankingService implements RankingService {

    /** The id of this service. */
    private static final String SERVICE_ID = "alexa";

    /** The estimated constant of rank x traffic. */
    private static final Long RANK_TRAFFIC_CONSTANT = 476408394L;

    /** Page views per visitor. */
    private static final Double PAGE_VIEWS_PER_VISITOR = 2.2;

    /** The ranking value types of this service. */
    public static final RankingType POPULARITY_RANK = new RankingType("alexa_rank", "Alexa Rank", "");
    public static final RankingType DAILY_VISITORS = new RankingType("alexa_daily_visitors", "Daily Visitors", "");
    public static final RankingType DAILY_PAGE_VIEWS = new RankingType("alexa_daily_page_views", "Daily Page Views", "");

    /** All available ranking types by AlexaRank. */
    private static final List<RankingType> RANKING_TYPES = Arrays.asList(POPULARITY_RANK, DAILY_VISITORS,
            DAILY_PAGE_VIEWS);

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();

        try {
            String encUrl = UrlHelper.encodeParameter(url);
            HttpResult httpResult = retriever.httpGet("http://data.alexa.com/data?cli=10&dat=s&url=" + encUrl);
            DocumentParser xmlParser = ParserFactory.createXmlParser();
            Document doc = xmlParser.parse(httpResult);

            Node popularityNode = XPathHelper.getNode(doc, "/ALEXA/SD/POPULARITY/@TEXT");
            if (popularityNode != null) {
                String popularity = popularityNode.getNodeValue();
                Float rank = Float.valueOf(popularity);
                results.put(POPULARITY_RANK, rank);
                float visitors = (float)Math.floor(RANK_TRAFFIC_CONSTANT / rank);
                results.put(DAILY_VISITORS, visitors);
                results.put(DAILY_PAGE_VIEWS, (float)(Math.floor(PAGE_VIEWS_PER_VISITOR * visitors)));
            } else {
                results.put(POPULARITY_RANK, 0f);
                results.put(DAILY_VISITORS, 0f);
                results.put(DAILY_PAGE_VIEWS, 0f);
            }
        } catch (HttpException e) {
            throw new RankingServiceException(e);
        } catch (ParserException e) {
            throw new RankingServiceException(e);
        }

        return new Ranking(this, url, results);
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
        AlexaRank ar = new AlexaRank();
        Ranking ranking = null;

        ranking = ar.getRanking("http://palladian.ws");
        System.out.println(ranking);
        System.out.println(ranking.getValues().get(AlexaRank.POPULARITY_RANK) + " rank");
        System.out.println(ranking.getValues().get(AlexaRank.DAILY_VISITORS) + " daily visitors");
        System.out.println(ranking.getValues().get(AlexaRank.DAILY_PAGE_VIEWS) + " daily page views");
    }

}
