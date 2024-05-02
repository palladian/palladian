package ws.palladian.retrieval.configuration;

/** @since 3.0.0 */
public final class StringConfigurationOption extends BaseConfigurationOption<String> {
    public StringConfigurationOption(String name, String key) {
        super(String.class, name, key, null);
    }
    public StringConfigurationOption(String name, String key, String defaultValue) {
        super(String.class, name, key, defaultValue);
    }
    public StringConfigurationOption(String name, String key, String defaultValue, boolean required) {
        super(String.class, name, key, defaultValue, required);
    }
}
