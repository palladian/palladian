package ws.palladian.classification.numeric;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * This stores the max - min differences for each feature of the training instances. We need these values to
 * normalize test or unseen data. <featureIndex, max-min>
 */
public class MinMaxNormalization implements Serializable {

    private static final long serialVersionUID = 7227377881428315427L;

    private Map<String, Double> normalizationMap = new HashMap<String, Double>();
    private Map<String, Double> minValueMap = new HashMap<String, Double>();

    public Map<String, Double> getNormalizationMap() {
        return normalizationMap;
    }

    public void setNormalizationMap(Map<String, Double> normalizationMap) {
        this.normalizationMap = normalizationMap;
    }

    public void setMinValueMap(Map<String, Double> minValueMap) {
        this.minValueMap = minValueMap;
    }

    public Map<String, Double> getMinValueMap() {
        return minValueMap;
    }

}