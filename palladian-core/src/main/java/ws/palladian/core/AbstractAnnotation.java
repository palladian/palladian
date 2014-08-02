package ws.palladian.core;

/**
 * <p>
 * Common functionality for {@link Annotation} interface.
 * </p>
 * 
 * @author Philipp Katz
 */
public abstract class AbstractAnnotation implements Annotation {

    @Override
    public final int compareTo(Annotation other) {
        // return Integer.valueOf(this.getStartPosition()).compareTo(other.getStartPosition());
        int result = Integer.valueOf(getStartPosition()).compareTo(other.getStartPosition());
        if (result == 0) {
            result = Integer.valueOf(other.getEndPosition()).compareTo(getEndPosition());
        }
        return result;
    }

    @Override
    public final int getEndPosition() {
        return getStartPosition() + getValue().length();
    }

    @Override
    public final boolean overlaps(Annotation other) {
        boolean overlaps = false;
        overlaps |= getStartPosition() <= other.getStartPosition() && getEndPosition() >= other.getStartPosition();
        overlaps |= getStartPosition() <= other.getEndPosition() && getEndPosition() >= other.getStartPosition();
        return overlaps;
    }

    @Override
    public final boolean congruent(Annotation other) {
        boolean congruent = true;
        congruent &= getStartPosition() == other.getStartPosition();
        congruent &= getEndPosition() == other.getEndPosition();
        return congruent;
    }

    @Override
    public final boolean sameTag(Annotation other) {
        return getTag().equalsIgnoreCase(other.getTag());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getEndPosition();
        result = prime * result + getStartPosition();
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        Annotation other = (Annotation)obj;
        if (getEndPosition() != other.getEndPosition()) {
            return false;
        }
        if (getStartPosition() != other.getStartPosition()) {
            return false;
        }
        return true;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Annotation [value=");
        builder.append(getValue());
        builder.append(", tag=");
        builder.append(getTag());
        builder.append(", span=");
        builder.append(getStartPosition());
        builder.append("-");
        builder.append(getEndPosition());
        builder.append("]");
        return builder.toString();
    }

}
