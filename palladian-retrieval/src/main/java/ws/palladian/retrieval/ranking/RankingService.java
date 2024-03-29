package ws.palladian.retrieval.ranking;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * <p>
 * Interface for ranking service implementations.
 * </p>
 *
 * @author Julien Schmehl
 */
public interface RankingService {

    /**
     * <p>
     * Get ranking values for a single URL.
     * </p>
     *
     * @param url
     * @return A map of ranking values for each type
     * @throws RankingServiceException In case of an error while retrieving the ranking.
     */
    Ranking getRanking(String url) throws RankingServiceException;

    /**
     * <p>
     * Get ranking values for a batch of URLs. Subclasses may offer a specific implementation for this case, if
     * supported by the underlying service or may just iterate over the parameters and use {@link #getRanking(String)}
     * to build the result.
     * </p>
     *
     * @param urls A list of URLs
     * @return A map of ranking values per URL
     * @throws RankingServiceException In case of an error while retrieving the ranking.
     */
    Map<String, Ranking> getRanking(Collection<String> urls) throws RankingServiceException;

    /**
     * <p>
     * Get the id of this ranking service.
     * </p>
     *
     * @return The id-string of this service
     */
    String getServiceId();

    /**
     * <p>
     * Get all ranking types of this ranking service.
     * </p>
     *
     * @return A list of ranking types
     */
    List<RankingType> getRankingTypes();

    /**
     * <p>
     * Get the ranking type for this id.
     * </p>
     *
     * @return The ranking type for the given id, or <code>null</code> if no such {@link RankingType}
     */
    RankingType getRankingType(String id);

}
