package ws.palladian.core;

public abstract class AbstractToken implements Token {

    @Override
    public final int compareTo(Token other) {
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
    public final boolean overlaps(Token other) {
        boolean overlaps = false;
        overlaps |= getStartPosition() <= other.getStartPosition() && getEndPosition() >= other.getStartPosition();
        overlaps |= getStartPosition() <= other.getEndPosition() && getEndPosition() >= other.getStartPosition();
        return overlaps;
    }

    @Override
    public final boolean congruent(Token other) {
        boolean congruent = true;
        congruent &= getStartPosition() == other.getStartPosition();
        congruent &= getEndPosition() == other.getEndPosition();
        return congruent;
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

}
