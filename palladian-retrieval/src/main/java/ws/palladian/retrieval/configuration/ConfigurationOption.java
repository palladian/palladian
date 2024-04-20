package ws.palladian.retrieval.configuration;

import java.util.Map;

/** @since 3.0.0 */
public interface ConfigurationOption<T> {
    /** @return Type of the config option (currently only used: String). */
    Class<T> getType();

    /**
     * @return A human-readable name of the configuration option (e.g. 'API Key')
     *         which can be presented in the UI.
     */
    String getName();

    /** @return Unique identifier of the config option (e.g. 'apikey') */
    String getKey();

    T get(Map<ConfigurationOption<?>, ?> config);
}
