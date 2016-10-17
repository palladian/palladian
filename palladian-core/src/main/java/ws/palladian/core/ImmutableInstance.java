package ws.palladian.core;

public final class ImmutableInstance extends AbstractInstance {

    private final FeatureVector vector;
    private final String category;
    private final int weight;

    public ImmutableInstance(FeatureVector vector, String category) {
    	this(vector, category, 1);
    }
    
    public ImmutableInstance(FeatureVector vector, String category, int weight) {
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

}
