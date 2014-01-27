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

    public ImmutableCategory(String name, double probability) {
        Validate.notNull(name, "name must not be null");
        Validate.inclusiveBetween(0., 1., probability, "probability must be in range [0,1]");
        this.probability = probability;
        this.name = name;
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
    public String toString() {
        return name + "=" + probability;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        long temp;
        temp = Double.doubleToLongBits(probability);
        result = prime * result + (int)(temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ImmutableCategory other = (ImmutableCategory)obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        if (Double.doubleToLongBits(probability) != Double.doubleToLongBits(other.probability))
            return false;
        return true;
    }

}
