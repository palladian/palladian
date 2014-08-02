package ws.palladian.core;

import static ws.palladian.classification.text.PalladianTextClassifier.VECTOR_TEXT_IDENTIFIER;

import java.util.Map;

import org.apache.commons.lang3.Validate;

import ws.palladian.core.value.ImmutableBooleanValue;
import ws.palladian.core.value.ImmutableDoubleValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.ImmutableTextValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Vector.VectorEntry;

public final class InstanceBuilder {

    private final Map<String, Value> valueMap = CollectionHelper.newHashMap();

    public InstanceBuilder set(String name, double value) {
        Validate.notEmpty(name, "name must not be empty");
        valueMap.put(name, new ImmutableDoubleValue(value));
        return this;
    }

    public InstanceBuilder set(String name, String value) {
        Validate.notEmpty(name, "name must not be empty");
        Validate.notNull(value, "value must not be null");
        valueMap.put(name, new ImmutableStringValue(value));
        return this;
    }

    public InstanceBuilder set(String name, boolean value) {
        Validate.notEmpty(name, "name must not be empty");
        valueMap.put(name, ImmutableBooleanValue.create(value));
        return this;
    }

    public InstanceBuilder set(String name, Value value) {
        Validate.notEmpty(name, "name must not be empty");
        Validate.notNull(value, "value must not be null");
        valueMap.put(name, value);
        return this;
    }

    public InstanceBuilder setText(String text) {
        Validate.notNull(text, "text must not be null");
        valueMap.put(VECTOR_TEXT_IDENTIFIER, new ImmutableTextValue(text));
        return this;
    }

    public InstanceBuilder setNull(String name) {
        Validate.notEmpty(name, "name must not be empty");
        valueMap.put(name, NullValue.NULL);
        return this;
    }

    public InstanceBuilder add(FeatureVector featureVector) {
        Validate.notNull(featureVector, "featureVector must not be null");
        for (VectorEntry<String, Value> entry : featureVector) {
            set(entry.key(), entry.value());
        }
        return this;
    }

    public FeatureVector create() {
        return new ImmutableFeatureVector(valueMap);
    }

    public Instance create(String category) {
        Validate.notEmpty(category, "category must not be empty");
        return new ImmutableInstance(create(), category);
    }

    public Instance create(boolean category) {
        return new ImmutableInstance(create(), category);
    }

    @Override
    public String toString() {
        return valueMap.toString();
    }

}
