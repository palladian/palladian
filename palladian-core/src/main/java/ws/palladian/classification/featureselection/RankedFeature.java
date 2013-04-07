/**
 * Created on: 05.02.2013 16:56:39
 */
package ws.palladian.classification.featureselection;

/**
 * @author Klemens Muthmann
 * @version 1.0
 * @since 0.2.0
 */
public final class RankedFeature {
    private String identifier;

    private double score;
    private String value;

    public RankedFeature(String identifier, String value, double score) {
        this.identifier = identifier;
        this.value = value;
        this.score = score;
    }

    public String getIdentifier() {
        return identifier;
    }

    public double getScore() {
        return score;
    }

    public String getValue() {
        return value;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("RankedFeature [identifier=");
        builder.append(identifier);
        builder.append(", score=");
        builder.append(score);
        builder.append(", value=");
        builder.append(value);
        builder.append("]");
        return builder.toString();
    }

}
