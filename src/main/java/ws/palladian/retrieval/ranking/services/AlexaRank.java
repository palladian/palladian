package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import ws.palladian.helper.html.XPathHelper;
import ws.palladian.helper.nlp.StringHelper;
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
public class AlexaRank extends BaseRankingService implements RankingService {

    /** The id of this service. */
    private static final String SERVICE_ID = "alexa";

    /** The ranking value types of this service. */
    static RankingType POPULARITY_RANK = new RankingType("alexa_rank", "Alexa Rank", "");

    static List<RankingType> RANKING_TYPES = new ArrayList<RankingType>();
    static {
        RANKING_TYPES.add(POPULARITY_RANK);
    }

    @Override
    public Ranking getRanking(String url) {

        Map<RankingType, Float> results = new HashMap<RankingType, Float>();

        String encUrl = StringHelper.urlEncode(url);
        Document doc = retriever.getXMLDocument("http://data.alexa.com/data?cli=10&dat=s&url=" + encUrl);

        if (doc != null) {
            Node popularityNode = XPathHelper.getNode(doc, "/ALEXA/SD/POPULARITY/@TEXT");
            if (popularityNode != null) {
                String popularity = popularityNode.getNodeValue();
                results.put(POPULARITY_RANK, Float.valueOf(popularity));
            } else {
                results.put(POPULARITY_RANK, 0f);
            }
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

    // public static void main(String[] args) {
    // RankingService alexa = new AlexaRank();
    // Ranking ranking = alexa.getRanking("http://www.engadget.com/2010/05/07/how-would-you-change-apples-ipad/");
    // System.out.println(ranking);
    // }

}
