package ws.palladian.core.dataset;

import ws.palladian.core.Instance;
import ws.palladian.core.dataset.FeatureInformation.FeatureInformationEntry;
import ws.palladian.core.value.ImmutableDoubleValue;
import ws.palladian.core.value.ImmutableStringValue;
import ws.palladian.core.value.NullValue;
import ws.palladian.core.value.Value;
import ws.palladian.core.value.io.ValueParser;
import ws.palladian.helper.collection.Vector.VectorEntry;
import ws.palladian.helper.functional.Factory;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Predicate;

public class FeatureInformationBuilder implements Factory<FeatureInformation> {

    private final Map<String, Class<? extends Value>> nameValues;

    public FeatureInformationBuilder() {
        this(new LinkedHashMap<String, Class<? extends Value>>());
    }

    private FeatureInformationBuilder(Map<String, Class<? extends Value>> nameValues) {
        this.nameValues = nameValues;
    }

    // TODO deprecate those methods which take a Class<? extends Value> argument

    public FeatureInformationBuilder set(String name, ValueParser valueDefinition) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(valueDefinition, "valueType must not be null");
        return set(name, valueDefinition.getType());
    }

    public FeatureInformationBuilder set(String name, Class<? extends Value> valueType) {
        Objects.requireNonNull(name, "name must not be null");
        Objects.requireNonNull(valueType, "valueType must not be null");
        nameValues.put(name, valueType);
        return this;
    }

    public FeatureInformationBuilder set(Iterable<String> names, ValueParser valueDefinition) {
        Objects.requireNonNull(names, "names must not be null");
        Objects.requireNonNull(valueDefinition, "valueType must not be null");
        for (String name : names) {
            set(name, valueDefinition);
        }
        return this;
    }

    public FeatureInformationBuilder set(Iterable<String> names, Class<? extends Value> valueType) {
        Objects.requireNonNull(names, "names must not be null");
        Objects.requireNonNull(valueType, "valueType must not be null");
        for (String name : names) {
            set(name, valueType);
        }
        return this;
    }

    public FeatureInformationBuilder set(FeatureInformationEntry infoEntry) {
        Objects.requireNonNull(infoEntry, "infoEntry must not be null");
        set(infoEntry.getName(), infoEntry.getType());
        return this;
    }

    public FeatureInformationBuilder add(FeatureInformation other) {
        Objects.requireNonNull(other, "other must not be null");
        for (FeatureInformationEntry entry : other) {
            nameValues.put(entry.getName(), entry.getType());
        }
        return this;
    }

    public FeatureInformationBuilder remove(String name) {
        Objects.requireNonNull(name, "name must not be null");
        nameValues.remove(name);
        return this;
    }

    public FeatureInformationBuilder filter(Predicate<? super String> filter) {
        Objects.requireNonNull(filter, "filter must not be null");
        Iterator<Entry<String, Class<? extends Value>>> iterator = nameValues.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<String, Class<? extends Value>> current = iterator.next();
            if (!filter.test(current.getKey())) {
                iterator.remove();
            }
        }
        return this;
    }

    /**
     * Build feature information from a given dataset of instances.
     *
     * @param instances The instances.
     * @return The builder for method chaining.
     * @throws IllegalArgumentException In case, the {@link Value} types in the given instances are
     *                                  incompatible (e.g. once {@link ImmutableStringValue}, once
     *                                  {@link ImmutableDoubleValue} for the same feature).
     */
    public static FeatureInformationBuilder fromInstances(Iterable<? extends Instance> instances) {
        Objects.requireNonNull(instances, "instances must not be null");
        Map<String, Class<? extends Value>> temp = new LinkedHashMap<>();
        for (Instance instance : instances) {
            for (VectorEntry<String, Value> entry : instance.getVector()) {
                Class<? extends Value> assignedType = temp.get(entry.key());
                if (assignedType == null) {
                    temp.put(entry.key(), entry.value().getClass());
                } else if (assignedType.equals(NullValue.class)) {
                    // we've currently stored a NullValue, try to replace it by a concrete value
                    temp.put(entry.key(), entry.value().getClass());
                } else if (!entry.value().getClass().equals(NullValue.class)) {
                    // only enforce check, if the current value is not a NullValue
                    if (!assignedType.equals(entry.value().getClass())) {
                        throw new IllegalArgumentException(
                                "Type for feature " + entry.key() + " " + entry.value().getClass().getName() + " does not equal existing type " + assignedType.getName());
                    }
                }
            }
        }
        return new FeatureInformationBuilder(temp);
    }

    @Override
    public FeatureInformation create() {
        return new ImmutableFeatureInformation(new LinkedHashMap<>(nameValues));
    }

}
