package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.configuration.Configuration;
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
public final class CompositeRankingService extends BaseRankingService implements RankingService {

    /** The logger for this class. */
    private static final Logger LOGGER = Logger.getLogger(CompositeRankingService.class);

    private final List<RankingService> rankingServices;

    public CompositeRankingService(Configuration config) {
        rankingServices = new ArrayList<RankingService>();
        rankingServices.add(new AlexaRank());
        rankingServices.add(new BibsonomyBookmarks(config));
        rankingServices.add(new BitlyClicks(config));
        rankingServices.add(new Compete(config));
//        rankingServices.add(new DeliciousBookmarks()); // FIXME
        rankingServices.add(new DiggStats());
        rankingServices.add(new FacebookLinkStats());
        rankingServices.add(new FriendfeedAggregatedStats());
        rankingServices.add(new FriendfeedStats());
        rankingServices.add(new GooglePageRank());
        rankingServices.add(new MajesticSeo(config));
        rankingServices.add(new PlurkPosts(config));
        rankingServices.add(new RedditStats());
        rankingServices.add(new SharethisStats(config));
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

    public Map<RankingService, Ranking> getRankings(String url) {
        Map<RankingService, Ranking> rankings = new HashMap<RankingService, Ranking>();

        for (RankingService rankingService : rankingServices) {
            Ranking ranking = rankingService.getRanking(url);
            LOGGER.debug("retrieved " + ranking);
            rankings.put(rankingService, ranking);
        }

        return rankings;
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

}
