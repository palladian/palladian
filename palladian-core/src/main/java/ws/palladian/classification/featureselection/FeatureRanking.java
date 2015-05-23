package ws.palladian.classification.featureselection;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @author Klemens Muthmann
 */
public final class FeatureRanking {

    private boolean isSorted;

    private final List<RankedFeature> rankedFeatures;

    public FeatureRanking() {
        this.rankedFeatures = new LinkedList<RankedFeature>();
        this.isSorted = false;
    }

    public FeatureRanking(Map<String, ? extends Number> values) {
        this();
        for (Entry<String, ? extends Number> entry : values.entrySet()) {
            add(entry.getKey(), entry.getValue().doubleValue());
        }
    }

    public void add(String featureIdentifier, double score) {
        rankedFeatures.add(new RankedFeature(featureIdentifier, score));
        isSorted = false;
    }

    public List<RankedFeature> getAboveThreshold(double threshold) {
        sort();
        RankedFeature key = new RankedFeature("dummy", threshold);
        int n = Collections.binarySearch(rankedFeatures, key);
        return getTopN(n);
    }

    public List<RankedFeature> getAll() {
        sort();
        return Collections.unmodifiableList(rankedFeatures);
    }

    public List<RankedFeature> getTopN(int n) {
        sort();
        if (rankedFeatures.size() < n) {
            return rankedFeatures;
        } else {
            return rankedFeatures.subList(0, n);
        }
    }

    /**
     * @param percent A number between 0.0 and 100.0
     * @return The top percent of the ranked features.
     */
    public List<RankedFeature> getTopPercent(float percent) {
        int n = Math.round(rankedFeatures.size() * percent / 100.0f);
        return getTopN(n);
    }

    public RankedFeature getFeature(String featureName) {
        for (RankedFeature rankedFeature : rankedFeatures) {
            if (rankedFeature.getName().equals(featureName)) {
                return rankedFeature;
            }
        }
        return null;
    }

    private void sort() {
        if (isSorted) {
            return;
        } else {
            Collections.sort(rankedFeatures);
            isSorted = true;
        }
    }

    public int size() {
        return rankedFeatures.size();
    }

    @Override
    public String toString() {
        sort();
        StringBuilder builder = new StringBuilder();
        builder.append("FeatureRanking [rankedFeatures=");
        builder.append(rankedFeatures);
        builder.append("]");
        return builder.toString();
    }
}
