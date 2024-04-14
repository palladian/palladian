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

    /** @since 3.0.0 */
    public interface ConfigurationOption {
        /** @return Type of the config option (currently only used: String). */
        Class<?> getType();

        /**
         * @return A human-readable name of the configuration option (e.g. 'API Key')
         *         which can be presented in the UI.
         */
        String getName();

        /** @return Unique identifier of the config option (e.g. 'apikey') */
        String getKey();
    }

    /**
     * Meta information and factory for a ranking service. It describes the ranking
     * types, configuration options, and allows to instantiate the service.
     *
     * @since 3.0.0
     */
    public interface RankingServiceMetaInfo<R extends RankingService> {
        /** @return All ranking types of this ranking service. */
        List<RankingType> getRankingTypes();

        /** @return The human-readable name of the ranking service. */
        String getServiceName();

        /** @return The ID of this ranking service. */
        String getServiceId();

        /** @return Config options which are required for this ranking service. */
        List<ConfigurationOption> getConfigurationOptions();

        /**
         * Instantiate a new ranking service.
         *
         * @param config The configuration (see {@link #getConfigurationOptions()})
         * @return The ranking service instance.
         */
        R create(Map<ConfigurationOption, ?> config);
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
     * @deprecated Get via {@link RankingServiceMetaInfo}
     */
    @Deprecated
    List<RankingType> getRankingTypes();

    /**
     * <p>
     * Get the ranking type for this id.
     * </p>
     *
     * @return The ranking type for the given id, or <code>null</code> if no such {@link RankingType}
     * @deprecated Get via {@link RankingServiceMetaInfo}
     */
    @Deprecated
    RankingType getRankingType(String id);

}
