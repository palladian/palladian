package ws.palladian.core;

final class ImmutableInstance implements Instance {

    private final FeatureVector vector;
    private final String category;

    ImmutableInstance(FeatureVector vector, String category) {
        this.vector = vector;
        this.category = category;
    }

    ImmutableInstance(FeatureVector vector, boolean category) {
        this(vector, String.valueOf(category));
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
    public String toString() {
        return vector + "=" + category;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + category.hashCode();
        result = prime * result + vector.hashCode();
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
        return true;
    }

}
