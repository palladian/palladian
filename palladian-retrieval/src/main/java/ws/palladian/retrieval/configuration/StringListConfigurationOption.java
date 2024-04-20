package ws.palladian.retrieval.configuration;

import java.util.List;

/** @since 3.0.0 */
public final class StringListConfigurationOption extends BaseConfigurationOption<List<String>> {
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public StringListConfigurationOption(String name, String key) {
        super((Class<List<String>>) ((Class)List.class), name, key, null);
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public StringListConfigurationOption(String name, String key, List<String> defaultValue) {
        super((Class<List<String>>) ((Class)List.class), name, key, defaultValue);
    }
}
