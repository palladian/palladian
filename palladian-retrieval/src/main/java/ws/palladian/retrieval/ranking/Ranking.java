package ws.palladian.retrieval.ranking;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.functional.Factory;

/**
 * <p>
 * Represents a ranking value retrieved at a given moment for a given RankingService.
 * </p>
 *
 * @author Julien Schmehl
 * @author pk
 */
public class Ranking {

    public static class Builder implements Factory<Ranking> {

        private final RankingService service;
        private final String url;
        private final Map<RankingType, Number> values;

        public Builder(RankingService service, String url) {
            this.service = service;
            this.url = url;
            this.values = CollectionHelper.newHashMap();
        }

        public Builder add(RankingType type, Number value) {
            this.values.put(type, value);
            return this;
        }

        public Builder addAll(Ranking ranking) {
            Set<Entry<RankingType, Number>> entries = ranking.getValues().entrySet();
            for (Entry<RankingType, Number> entry : entries) {
                add(entry.getKey(), entry.getValue());
            }
            return this;
        }

        @Override
        public Ranking create() {
            return new Ranking(service, url, values);
        }

        @Override
        public String toString() {
            return "Builder [service=" + service + ", url=" + url + ", values=" + values + "]";
        }

    }

    /** The ranking service producing this ranking */
    private final RankingService service;

    /** The ranking values */
    private final Map<RankingType, Number> values;

    /** The URL these ranking values are for */
    private final String url;

    /** The time when the ranking was retrieved */
    private final Date retrieved;

    /**
     * <p>
     * Create a new instance with the retrieved value set to now.
     * </p>
     *
     * @param service
     * @param url
     * @param values a Map of all ranking values associated with this ranking and their corresponding ranking type
     * @deprecated Use the {@link Builder}.
     */
    @Deprecated
    public Ranking(RankingService service, String url, Map<RankingType, ? extends Number> values) {
        this(service, url, values, new Date());
    }

    /**
     * <p>
     * Create a new, fully initialized instance.
     * </p>
     *
     * @param service
     * @param url
     * @param values a Map of all ranking values associated with this ranking and their corresponding ranking type
     * @param retrieved
     * @deprecated Use the {@link Builder}.
     */
    @Deprecated
    public Ranking(RankingService service, String url, Map<RankingType, ? extends Number> values, Date retrieved) {
        this.service = service;
        this.values = new HashMap<RankingType, Number>(values);
        this.url = url;
        this.retrieved = retrieved;
    }

    public RankingService getService() {
        return service;
    }

    /**
     * <p>
     * Get a Map of all ranking values associated with this ranking and their corresponding ranking type.
     * </p>
     *
     * @return pairs of ranking type and ranking value
     */
    public Map<RankingType, Number> getValues() {
        return Collections.unmodifiableMap(values);
    }

    public String getUrl() {
        return url;
    }

    public Date getRetrieved() {
        return retrieved;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ranking for ").append(getUrl());
        stringBuilder.append(" from ").append(getService().getServiceId()).append(":");
        for (Entry<RankingType, ? extends Number> entry : getValues().entrySet()) {
            RankingType rankingType = entry.getKey();
            Number rankingValue = entry.getValue();
            stringBuilder.append(" ").append(rankingType.getId()).append("=").append(rankingValue);
        }
        return stringBuilder.toString();
    }
}
