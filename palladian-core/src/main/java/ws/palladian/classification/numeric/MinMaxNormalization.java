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

    private Map<Integer, Double> normalizationMap = new HashMap<Integer, Double>();
    private Map<Integer, Double> minValueMap = new HashMap<Integer, Double>();

    public Map<Integer, Double> getNormalizationMap() {
        return normalizationMap;
    }

    public void setNormalizationMap(Map<Integer, Double> normalizationMap) {
        this.normalizationMap = normalizationMap;
    }

    public void setMinValueMap(Map<Integer, Double> minValueMap) {
        this.minValueMap = minValueMap;
    }

    public Map<Integer, Double> getMinValueMap() {
        return minValueMap;
    }

}