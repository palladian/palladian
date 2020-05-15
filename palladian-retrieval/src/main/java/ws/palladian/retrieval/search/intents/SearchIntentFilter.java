package ws.palladian.retrieval.search.intents;

import java.util.Collection;
import java.util.HashSet;

public class SearchIntentFilter {
    private String key;
    private Collection<String> values = new HashSet<>();
    private String minDefinition;
    private String maxDefinition;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Collection<String> getValues() {
        return values;
    }

    public void setValues(Collection<String> values) {
        this.values = values;
    }

    public String getMinDefinition() {
        return minDefinition;
    }

    public void setMinDefinition(String minDefinition) {
        this.minDefinition = minDefinition;
    }

    public String getMaxDefinition() {
        return maxDefinition;
    }

    public void setMaxDefinition(String maxDefinition) {
        this.maxDefinition = maxDefinition;
    }
}