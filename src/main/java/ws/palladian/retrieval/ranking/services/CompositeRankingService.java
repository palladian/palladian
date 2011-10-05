package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * Group together multiple {@link RankingService}s.
 * </p>
 * 
 * @author Philipp Katz
 */
public class CompositeRankingService extends BaseRankingService implements RankingService {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(CompositeRankingService.class);

    private final List<RankingService> rankingServices;

    public CompositeRankingService() {
        rankingServices = new ArrayList<RankingService>();
        rankingServices.add(new AlexaRank());
        rankingServices.add(new BibsonomyBookmarks());
        rankingServices.add(new BitlyClicks());
        rankingServices.add(new Compete());
        rankingServices.add(new DeliciousBookmarks());
        rankingServices.add(new DiggStats());
        rankingServices.add(new FacebookLinkStats());
        rankingServices.add(new FriendfeedAggregatedStats());
        rankingServices.add(new FriendfeedStats());
        rankingServices.add(new GoogleBuzzShares());
        rankingServices.add(new GooglePageRank());
        rankingServices.add(new MajesticSeo());
        rankingServices.add(new PlurkPosts());
        rankingServices.add(new RedditStats());
        rankingServices.add(new SharethisStats());
        rankingServices.add(new TweetmemeStats());
        rankingServices.add(new WebOfTrust());
    }

    public CompositeRankingService(Collection<RankingService> rankingServices) {
        this.rankingServices = new ArrayList<RankingService>(rankingServices);
    }

    @Override
    public Ranking getRanking(String url) {
        Map<RankingType, Float> rankings = new HashMap<RankingType, Float>();
        for (RankingService rankingService : rankingServices) {
            Ranking ranking = rankingService.getRanking(url);
            LOGGER.debug("retrieved " + ranking);
            rankings.putAll(ranking.getValues());
        }
        Ranking ranking = new Ranking(this, url, rankings);
        return ranking;
    }

    @Override
    public String getServiceId() {
        return "compositeRankingService";
    }

    @Override
    public List<RankingType> getRankingTypes() {
        List<RankingType> rankingTypes = new ArrayList<RankingType>();
        for (RankingService rankingService : rankingServices) {
            rankingTypes.addAll(rankingService.getRankingTypes());
        }
        return rankingTypes;
    }

    public static void main(String[] args) {
        // String url = "http://www.howtouse-photoshop.com/2011/09/28/27-photoshop-video-tutorials/";
        // CompositeRankingService compositeRankingService = new CompositeRankingService();
        // Ranking ranking = compositeRankingService.getRanking(url);
        // System.out.println(ranking);
    }
}
