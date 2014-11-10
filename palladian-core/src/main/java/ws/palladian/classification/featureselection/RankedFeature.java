package ws.palladian.classification.featureselection;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * @author Klemens Muthmann
 * @author Philipp Katz
 */
public final class RankedFeature implements Comparable<RankedFeature> {
    private final String name;
    private final double score;

    public RankedFeature(String name, double score) {
        this.name = name;
        this.score = score;
    }

    public String getName() {
        return name;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        NumberFormat format = NumberFormat.getNumberInstance(Locale.US);
        format.setMaximumFractionDigits(5);
        return name + "=" + format.format(score);
    }

    @Override
    public int compareTo(RankedFeature other) {
        int scoreComparison = Double.compare(other.score, this.score);
        if (scoreComparison == 0) {
            return this.name.compareTo(other.name);
        }
        return scoreComparison;
    }

}
