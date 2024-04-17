package ws.palladian.helper.math;

public class Range<V> {
    private V min;
    private V max;

    public Range(V min, V max) {
        this.min = min;
        this.max = max;
    }

    public V getMin() {
        return min;
    }

    public void setMin(V min) {
        this.min = min;
    }

    public V getMax() {
        return max;
    }

    public void setMax(V max) {
        this.max = max;
    }

    @Override
    public String toString() {
        return "Range{" + "min=" + min + ", max=" + max + '}';
    }
}
