package ws.palladian.retrieval.ranking.services;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.retrieval.HttpRetriever;
import ws.palladian.retrieval.HttpRetrieverFactory;
import ws.palladian.retrieval.ranking.Ranking;
import ws.palladian.retrieval.ranking.RankingService;
import ws.palladian.retrieval.ranking.RankingServiceException;
import ws.palladian.retrieval.ranking.RankingType;

/**
 * <p>
 * Base implementation for {@link RankingService}s.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class BaseRankingService implements RankingService {
    
    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseRankingService.class);

    /** DocumentRetriever for HTTP downloading purposes. */
    protected final HttpRetriever retriever;

    public BaseRankingService() {
        retriever = HttpRetrieverFactory.getHttpRetriever();
        
        // we use a rather short timeout here, as responses are short.
        retriever.setConnectionTimeout(5000);
    }

    /**
     * <p>
     * Same as getRanking but here we swallow the exception as the caller can not act on it anyway.
     * </p>
     * 
     * @param url The url to rank.
     * @return The ranking or null if an error occurred.
     */
    public Ranking tryGetRanking(String url) {
        Ranking ranking = null;

        try {
            ranking = getRanking(url);
        } catch (RankingServiceException e) {
            LOGGER.warn("Encountered exception while getting ranking via {}: {}", getClass().getSimpleName(), e);
        }

        return ranking;
    }

    @Override
    public Map<String, Ranking> getRanking(List<String> urls) throws RankingServiceException {
        Map<String, Ranking> results = new HashMap<String, Ranking>();
        if (!isBlocked()) {
            // iterate through urls and get ranking for each
            for (String url : urls) {
                results.put(url, getRanking(url));
            }
        }
        return results;
    }

    @Override
    public RankingType getRankingType(String id) {
        List<RankingType> rankingTypes = getRankingTypes();
        for (RankingType rankingType : rankingTypes) {
            if (rankingType.getId().equals(id)) {
                return rankingType;
            }
        }
        return null;
    }

    @Override
    public boolean checkBlocked() {
        return false;
    }

    @Override
    public boolean isBlocked() {
        return false;
    }

    @Override
    public void resetBlocked() {
    }

}
