package ws.palladian.retrieval.facebook;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import ws.palladian.retrieval.facebook.FacebookInsights.Period;

public final class Insights implements Iterable<Value> {
    private final String name;
    private final Period period;
    private final List<Value> values;
    private final String title;
    private final String description;
    private final String id;

    Insights(String name, Period period, List<Value> values, String title, String description, String id) {
        this.name = name;
        this.period = period;
        this.values = values;
        this.title = title;
        this.description = description;
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public Period getPeriod() {
        return period;
    }

    public List<Value> getValues() {
        return Collections.unmodifiableList(values);
    }

    /**
     * @return The names of all values within this instance, in case the values are objects, empty set in case only one
     *         value is attached.
     */
    public Set<String> getValueNames() {
        Set<String> valueNames = new TreeSet<>();
        for (Value value : this) {
            if (value.getValue() instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> valueMap = (Map<String, Object>)value.getValue();
                valueNames.addAll(valueMap.keySet());
            }
        }
        return Collections.unmodifiableSet(valueNames);
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getId() {
        return id;
    }

    @Override
    public Iterator<Value> iterator() {
        return getValues().iterator();
    }

    @Override
    public String toString() {
        return "Insights [name=" + name + ", period=" + period + ", values=" + values + ", title=" + title
                + ", description=" + description + ", id=" + id + "]";
    }
}