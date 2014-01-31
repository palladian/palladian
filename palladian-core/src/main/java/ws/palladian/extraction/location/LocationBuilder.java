package ws.palladian.extraction.location;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.Validate;

import ws.palladian.helper.collection.Factory;
import ws.palladian.helper.constants.Language;

/**
 * Builder for {@link Location}s. The created instances are immutable.
 * 
 * @author pk
 */
public final class LocationBuilder implements Factory<Location> {

    private int id;
    private String primaryName;
    private Set<AlternativeName> alternativeNames;
    private LocationType type;
    private Long population;
    private List<Integer> ancestorIds;
    private GeoCoordinate coordinate;

    public LocationBuilder setId(int id) {
        this.id = id;
        return this;
    }

    public LocationBuilder setPrimaryName(String primaryName) {
        this.primaryName = primaryName;
        return this;
    }

    public LocationBuilder setAlternativeNames(Collection<? extends AlternativeName> alternativeNames) {
        this.alternativeNames = alternativeNames != null ? new HashSet<AlternativeName>(alternativeNames) : null;
        return this;
    }

    public LocationBuilder addAlternativeName(AlternativeName alternativeName) {
        Validate.notNull(alternativeName, "alternativeName must not be null");
        if (alternativeNames == null) {
            alternativeNames = new HashSet<AlternativeName>();
        }
        alternativeNames.add(alternativeName);
        return this;
    }

    public LocationBuilder addAlternativeName(String name, Language language) {
        Validate.notNull(name, "name must not be null");
        addAlternativeName(new AlternativeName(name, language));
        return this;
    }

    public LocationBuilder setType(LocationType type) {
        this.type = type;
        return this;
    }

    public LocationBuilder setPopulation(Long population) {
        this.population = population;
        return this;
    }

    public LocationBuilder setAncestorIds(List<Integer> ancestorIds) {
        this.ancestorIds = ancestorIds;
        return this;
    }

    public LocationBuilder setAncestorIds(Integer... ancestorIds) {
        this.ancestorIds = Arrays.asList(ancestorIds);
        return this;
    }

    public LocationBuilder setCoordinate(GeoCoordinate coordinate) {
        this.coordinate = coordinate;
        return this;
    }

    public LocationBuilder setCoordinate(double lat, double lng) {
        this.coordinate = new ImmutableGeoCoordinate(lat, lng);
        return this;
    }

    @Override
    public Location create() {
        return new ImmutableLocation(id, primaryName, alternativeNames, type, coordinate, population, ancestorIds);
    }

}
