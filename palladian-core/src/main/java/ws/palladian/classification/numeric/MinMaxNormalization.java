package ws.palladian.classification.numeric;

import java.io.Serializable;
import java.util.Map;

/**
 * This stores the max - min differences for each feature of the training instances. We need these values to
 * normalize test or unseen data. <featureIndex, max-min>
 */
public class MinMaxNormalization implements Serializable {

    private static final long serialVersionUID = 7227377881428315427L;

    private final Map<String, Double> normalizationMap;
    private final Map<String, Double> minValueMap;

    /**
     * @param normalizationMap
     * @param minValueMap
     */
    public MinMaxNormalization(Map<String, Double> normalizationMap, Map<String, Double> minValueMap) {
        this.normalizationMap = normalizationMap;
        this.minValueMap = minValueMap;
    }

    public Map<String, Double> getNormalizationMap() {
        return normalizationMap;
    }

    public Map<String, Double> getMinValueMap() {
        return minValueMap;
    }
    
    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        for (String name : normalizationMap.keySet()) {
            toStringBuilder.append(name).append(";");
            toStringBuilder.append(normalizationMap.get(name)).append(";");
            toStringBuilder.append(minValueMap.get(name)).append("\n");
        }
        return toStringBuilder.toString();
    }

}