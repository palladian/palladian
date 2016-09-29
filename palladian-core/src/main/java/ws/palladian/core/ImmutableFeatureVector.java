package ws.palladian.core;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.EntryConverter;

final class ImmutableFeatureVector extends AbstractFeatureVector {

    private static final EntryConverter<String, Value> CONVERTER = new EntryConverter<>();

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
    public Collection<Value> values() {
        return valueMap.values();
    }

    @Override
    public String toString() {
        return valueMap.toString();
    }

}
