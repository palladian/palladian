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
    private final String identifier;
    private final double score;
    private final String value;

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

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((identifier == null) ? 0 : identifier.hashCode());
        result = prime * result + ((value == null) ? 0 : value.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        RankedFeature other = (RankedFeature)obj;
        if (identifier == null) {
            if (other.identifier != null)
                return false;
        } else if (!identifier.equals(other.identifier))
            return false;
        if (value == null) {
            if (other.value != null)
                return false;
        } else if (!value.equals(other.value))
            return false;
        return true;
    }

}
