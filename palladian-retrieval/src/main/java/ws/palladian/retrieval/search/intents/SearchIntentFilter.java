package ws.palladian.retrieval.search.intents;

import java.util.Collection;
import java.util.HashSet;

public class SearchIntentFilter {
    private String key;
    private Collection<String> values = new HashSet<>();
    private String minDefinition;
    private String maxDefinition;
    private Double margin;
    private Boolean booleanValue;
    private String unit;
    private Boolean without;

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

    public Boolean getBooleanValue() {
        return booleanValue;
    }

    public void setBooleanValue(Boolean booleanValue) {
        this.booleanValue = booleanValue;
    }

    public Double getMargin() {
        return margin;
    }

    public void setMargin(Double margin) {
        this.margin = margin;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Boolean getWithout() {
        return without;
    }

    public void setWithout(Boolean without) {
        this.without = without;
    }

    @Override
    public String toString() {
        return "SearchIntentFilter{" +
                "key='" + key + '\'' +
                ", values=" + values +
                ", minDefinition='" + minDefinition + '\'' +
                ", maxDefinition='" + maxDefinition + '\'' +
                ", margin='" + margin + '\'' +
                ", unit='" + unit + '\'' +
                ", booleanValue=" + booleanValue +
                ", without=" + without +
                '}';
    }
}
