package ws.palladian.retrieval.ranking;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

/**
 * <p>
 * Represents a ranking value retrieved at a given moment for a given RankingService.
 * </p>
 * 
 * @author Julien Schmehl
 */
public class Ranking {

    /** The ranking service producing this ranking */
    private final RankingService service;

    /** The ranking values */
    private final Map<RankingType, Float> values;

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
     */
    public Ranking(RankingService service, String url, Map<RankingType, Float> values) {
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
     */
    public Ranking(RankingService service, String url, Map<RankingType, Float> values, Date retrieved) {
        this.service = service;
        this.values = values;
        this.url = url;
        this.retrieved = retrieved;
    }

//    /**
//     * <p>
//     * Get the total of all ranking values associated with this ranking.
//     * </p>
//     * 
//     * @return the total sum of all ranking values
//     */
//    public float getRankingValueSum() {
//        float sum = 0;
//        for (RankingType rt : values.keySet()) {
//            sum += values.get(rt);
//        }
//        return sum;
//    }

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
    public Map<RankingType, Float> getValues() {
        return values;
    }

    public String getUrl() {
        return url;
    }

    public Date getRetrieved() {
        return retrieved;
    }

    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Ranking for ").append(getUrl());
        stringBuilder.append(" from ").append(getService().getServiceId()).append(":");
        for (Entry<RankingType, Float> entry : getValues().entrySet()) {
            RankingType rankingType = entry.getKey();
            Float rankingValue = entry.getValue();
            stringBuilder.append(" ").append(rankingType.getId()).append("=").append(rankingValue);
        }
        return stringBuilder.toString();
    }
}
