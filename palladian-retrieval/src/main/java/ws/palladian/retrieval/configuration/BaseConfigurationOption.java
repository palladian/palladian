package ws.palladian.retrieval.configuration;

import java.util.Map;

public abstract class BaseConfigurationOption<T> implements ConfigurationOption<T> {
    private final String name;
    private final String key;
    private final Class<T> type;
    private final T defaultValue;

    protected BaseConfigurationOption(Class<T> type, String name, String key, T defaultValue) {
        this.type = type;
        this.name = name;
        this.key = key;
        this.defaultValue = defaultValue;
    }

    @Override
    public final Class<T> getType() {
        return type;
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String getKey() {
        return key;
    }

    @SuppressWarnings("unchecked")
    @Override
    public final T get(Map<ConfigurationOption<?>, ? extends Object> config) {
        if (!config.containsKey(this)) {
            return defaultValue;
        }
        return (T) config.get(this);
    }
}
