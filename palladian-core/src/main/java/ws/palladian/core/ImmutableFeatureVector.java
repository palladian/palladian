package ws.palladian.core;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.EntryConverter;

final class ImmutableFeatureVector implements FeatureVector {

    private static final EntryConverter<String, Value> CONVERTER = new EntryConverter<String, Value>();

    private final Map<String, Value> valueMap;

    ImmutableFeatureVector(Map<String, Value> valueMap) {
        Validate.notNull(valueMap, "valueMap must not be null");
        this.valueMap = valueMap;
    }

    @Override
    public Iterator<VectorEntry<String, Value>> iterator() {
        return CollectionHelper.convert(valueMap.entrySet().iterator(), CONVERTER);
    }

    @Override
    public Value get(String k) {
        Validate.notEmpty(k, "k must not be empty");
        Value value = valueMap.get(k);
        return value != null ? value : NullValue.NULL;
    }

    @Override
    public int size() {
        return valueMap.size();
    }

    @Override
    public Set<String> keys() {
        return valueMap.keySet();
    }

    @Override
    public int hashCode() {
        return valueMap.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null || getClass() != obj.getClass()) {
            return false;
        }
        ImmutableFeatureVector other = (ImmutableFeatureVector)obj;
        return valueMap.equals(other.valueMap);
    }

    @Override
    public String toString() {
        return valueMap.toString();
    }

}
