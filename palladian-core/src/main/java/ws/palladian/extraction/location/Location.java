package ws.palladian.extraction.location;

import java.util.Collection;
import java.util.Set;

import ws.palladian.helper.collection.CollectionHelper;

public class Location {

    private Set<String> names;
    private String type;
    private Double latitude;
    private Double longitude;
    private Integer population;

    public Location() {
        names = CollectionHelper.newHashSet();
    }

    public String getName() {
        if (names.isEmpty()) {
            return "";
        }
        return getNames().iterator().next();
    }

    public Collection<String> getNames() {
        return names;
    }

    public void setNames(Set<String> names) {
        this.names = names;
    }

    public void addName(String name) {
        this.names.add(name);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Integer getPopulation() {
        return population;
    }

    public void setPopulation(Integer population) {
        this.population = population;
    }

}
