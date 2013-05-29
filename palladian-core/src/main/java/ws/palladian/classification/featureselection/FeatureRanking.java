/**
 * Created on: 05.02.2013 16:29:25
 */
package ws.palladian.classification.featureselection;

import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class FeatureRanking {

    /**
     * @author Klemens Muthmann
     * @version 1.0
     * @since 0.2.0
     */
    private final class FeatureRankingComparator implements Comparator<RankedFeature> {
        @Override
        public int compare(RankedFeature o1, RankedFeature o2) {
            return Double.compare(o2.getScore(), o1.getScore());
        }
    }

    private boolean isSorted;

    private List<RankedFeature> rankedFeatures;

    public FeatureRanking() {
        this.rankedFeatures = new LinkedList<RankedFeature>();
        this.isSorted = false;
    }

    public void add(String featureIdentifier, double score) {
        rankedFeatures.add(new RankedFeature("feature", featureIdentifier, score));
        isSorted = false;

    }

    public void addSparse(String featureIdentifier, String featureValue, double score) {
        rankedFeatures.add(new RankedFeature(featureIdentifier, featureValue, score));
        isSorted = false;
    }

    public List<RankedFeature> getAboveThreshold(double threshold) {
        sort();
        RankedFeature key = new RankedFeature("dummy", "dummy", threshold);
        int n = Collections.binarySearch(rankedFeatures, key, new FeatureRankingComparator());
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
        int n = Math.round((float)rankedFeatures.size() * percent / 100.0f);
        return getTopN(n);
    }

    private void sort() {
        if (isSorted) {
            return;
        } else {
            Collections.sort(rankedFeatures, new FeatureRankingComparator());
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
