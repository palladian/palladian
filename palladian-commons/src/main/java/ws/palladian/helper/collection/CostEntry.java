package ws.palladian.helper.collection;

public class CostEntry<S, T> {
    private S key;
    private T value;
    private int cost;

    public CostEntry(S key, T value, int cost) {
        this.key = key;
        this.value = value;
        this.cost = cost;
    }

    public S getKey() {
        return key;
    }

    public void setKey(S key) {
        this.key = key;
    }

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }
}
