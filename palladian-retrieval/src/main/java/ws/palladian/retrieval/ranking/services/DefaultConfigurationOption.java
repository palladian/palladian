package ws.palladian.retrieval.ranking.services;

import ws.palladian.retrieval.ranking.RankingService.ConfigurationOption;

public final class DefaultConfigurationOption implements ConfigurationOption {
    private final Class<?> type;
    private final String name;
    private final String key;

    public DefaultConfigurationOption(Class<?> type, String name, String key) {
        this.type = type;
        this.name = name;
        this.key = key;
    }

    @Override
    public Class<?> getType() {
        return type;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getKey() {
        return key;
    }
}