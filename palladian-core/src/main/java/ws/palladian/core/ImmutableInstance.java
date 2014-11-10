package ws.palladian.core;

final class ImmutableInstance implements Instance {

    private final FeatureVector vector;
    private final String category;
    private final int weight;

    ImmutableInstance(FeatureVector vector, String category, int weight) {
        this.vector = vector;
        this.category = category;
        this.weight = weight;
    }

    @Override
    public FeatureVector getVector() {
        return vector;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public int getWeight() {
        return weight;
    }

    @Override
    public String toString() {
        StringBuilder toStringBuilder = new StringBuilder();
        toStringBuilder.append(vector + "=" + category);
        if (weight > 1) {
            toStringBuilder.append(" (weight=").append(weight).append(")");
        }
        return toStringBuilder.toString();
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + category.hashCode();
        result = prime * result + vector.hashCode();
        long temp = Double.doubleToLongBits(weight);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImmutableInstance other = (ImmutableInstance)obj;
        if (!category.equals(other.category)) {
            return false;
        }
        if (!vector.equals(other.vector)) {
            return false;
        }
        if (weight != other.weight) {
            return false;
        }
        return true;
    }

}
