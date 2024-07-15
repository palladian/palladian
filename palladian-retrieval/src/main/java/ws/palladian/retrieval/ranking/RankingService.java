package ws.palladian.retrieval.ranking;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import ws.palladian.retrieval.configuration.ConfigurationOption;

/**
 * <p>
 * Interface for ranking service implementations.
 * </p>
 *
 * @author Julien Schmehl
 */
public interface RankingService {

    /**
     * Meta information and factory for a ranking service. It describes the ranking
     * types, configuration options, and allows to instantiate the service.
     *
     * @since 3.0.0
     */
    public interface RankingServiceMetaInfo<R extends RankingService> {
        /** @return The human-readable name of the ranking service. */
        String getServiceName();

        /** @return The ID of this ranking service. */
        String getServiceId();

        /** @return Config options which are required for this ranking service. */
        List<ConfigurationOption<?>> getConfigurationOptions();

        /**
         * Instantiate a new ranking service.
         *
         * @param config The configuration (see {@link #getConfigurationOptions()})
         * @return The ranking service instance.
         */
        R create(Map<ConfigurationOption<?>, ?> config);

        /**
         * @return A URL which represents the given service, respectively API - ideally
         *         it should link to a page with the API specification and / or a place
         *         where the user can create an API key. If no such documentation
         *         exists, return `null`.
         */
        String getServiceDocumentationUrl();
        
        /**
         * @return A short and to the point, plain text description of this service. At
         *         least one full grammatically correct English sentence ending with a
         *         full stop. If no description is available, return `null`.
         */
        String getServiceDescription();
    }

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
     * @deprecated Get via {@link RankingServiceMetaInfo}
     */
    @Deprecated
    String getServiceId();

    /**
     * <p>
     * Get all ranking types of this ranking service.
     * </p>
     *
     * @return A list of ranking types
     */
    List<RankingType<?>> getRankingTypes();

    /**
     * <p>
     * Get the ranking type for this id.
     * </p>
     *
     * @return The ranking type for the given id, or <code>null</code> if no such {@link RankingType}
     */
    RankingType<?> getRankingType(String id);

}
