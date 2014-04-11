package ws.palladian.core;

public final class ImmutableInstance implements Instance {

    private final FeatureVector vector;
    private final String category;

    public ImmutableInstance(FeatureVector vector, String category) {
        this.vector = vector;
        this.category = category;
    }

    public ImmutableInstance(FeatureVector vector, boolean category) {
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

}
