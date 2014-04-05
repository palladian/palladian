package ws.palladian.classification;

import java.util.Map.Entry;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.Function;

public final class ImmutableCategory implements Category {

    public static final class EntryConverter implements Function<Entry<String, Double>, Category> {
        @Override
        public Category compute(final Entry<String, Double> input) {
            return new ImmutableCategory(input.getKey(), input.getValue());
        }
    }

    private final String name;
    private final double probability;
    private final int count;

    public ImmutableCategory(String name, double probability) {
        this(name, probability, -1);
    }

    public ImmutableCategory(String name, double probability, int count) {
        Validate.notNull(name, "name must not be null");
//        Validate.inclusiveBetween(0., 1., probability, "probability must be in range [0,1], was %f", probability);
        Validate.isTrue(count == -1 || count >= 0, "count must be either -1, or greater/equal zero");
        this.probability = probability;
        this.name = name;
        this.count = count;
    }
    
    public ImmutableCategory(String name, int count, int totalCount) {
        Validate.notNull(name, "name must not be null");
        Validate.isTrue(count <= totalCount, "count must be less or equal totalCount");
        this.name = name;
        this.probability = totalCount > 0 ? (double)count/totalCount : 0;
        this.count = count;
    }

    @Override
    public double getProbability() {
        return probability;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public String toString() {
        return name + "=" + probability;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + name.hashCode();
        long temp;
        temp = Double.doubleToLongBits(probability);
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
        ImmutableCategory other = (ImmutableCategory)obj;
        if (!name.equals(other.name)) {
            return false;
        }
        if (Double.doubleToLongBits(probability) != Double.doubleToLongBits(other.probability)) {
            return false;
        }
        return true;
    }

    @Override
    public int compareTo(Category other) {
        return Double.compare(other.getProbability(), this.probability);
    }

}
