package ws.palladian.classification.featureselection;

/**
 * @author Klemens Muthmann
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
        return name + "=" + score;
    }

    @Override
    public int compareTo(RankedFeature other) {
        return Double.compare(other.score, this.score);
    }

}
