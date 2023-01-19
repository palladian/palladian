package ws.palladian.core;

public abstract class AbstractInstance implements Instance {

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append(getVector() + "=" + getCategory());
        if (getWeight() > 1) {
            toStringBuilder.append(" (weight=").append(getWeight()).append(")");
        }
        return toStringBuilder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + getCategory().hashCode();
        result = prime * result + getVector().hashCode();
        long temp = Double.doubleToLongBits(getWeight());
        result = prime * result + (int) (temp ^ (temp >>> 32));
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
        ImmutableInstance other = (ImmutableInstance) obj;
        if (!getCategory().equals(other.getCategory())) {
            return false;
        }
        if (!getVector().equals(other.getVector())) {
            return false;
        }
        if (getWeight() != other.getWeight()) {
            return false;
        }
        return true;
    }

}
