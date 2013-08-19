package ws.palladian.retrieval.ranking.services;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * Group together multiple {@link RankingService}s. In contrast to individual {@link RankingService} implementations,
 * this retrieval methods of this class do not throw {@link RankingServiceException}s; in case any of the involved
 * {@link RankingService}s fails, it is simply ignored and the remaining ranking results are being returned..
 * </p>
 * 
 * @author Philipp Katz
 */
public final class CompositeRankingService extends BaseRankingService implements RankingService {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(CompositeRankingService.class);

    private final List<RankingService> rankingServices;

    /**
     * <p>
     * Create a new {@link CompositeRankingService} with the specified available {@link RankingService}s.
     * </p>
     * 
     * @param rankingServices
     */
    public CompositeRankingService(Collection<RankingService> rankingServices) {
        this.rankingServices = new ArrayList<RankingService>(rankingServices);
    }

    @Override
    public Ranking getRanking(String url) throws RankingServiceException {
        Map<RankingType, Float> rankings = new HashMap<RankingType, Float>();
        for (RankingService rankingService : rankingServices) {
            try {
                Ranking ranking = rankingService.getRanking(url);
                LOGGER.debug("retrieved " + ranking);
                rankings.putAll(ranking.getValues());
            } catch (RankingServiceException e) {
                LOGGER.warn("Exception for {}", rankingService);
            }
        }
        return new Ranking(this, url, rankings);
    }

    public Map<RankingService, Ranking> getRankings(String url) throws RankingServiceException {
        Map<RankingService, Ranking> rankings = new HashMap<RankingService, Ranking>();
        for (RankingService rankingService : rankingServices) {
            try {
                Ranking ranking = rankingService.getRanking(url);
                LOGGER.debug("retrieved " + ranking);
                rankings.put(rankingService, ranking);
            } catch (RankingServiceException e) {
                LOGGER.warn("Exception for {}", rankingService);
            }
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
