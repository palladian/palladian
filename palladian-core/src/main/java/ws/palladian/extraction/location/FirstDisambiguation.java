package ws.palladian.extraction.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.PalladianLocationExtractor.LocationLookup;
import ws.palladian.extraction.location.PalladianLocationExtractor.LocationTypeFilter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.Filter;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.features.Annotated;

public class FirstDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(FirstDisambiguation.class);
    private final LocationSource locationSource;

    public FirstDisambiguation(LocationSource locationSource) {
        this.locationSource = locationSource;
    }

    @Override
    public List<LocationAnnotation> disambiguate(List<Annotated> taggedEntities, LocationLookup cache) {

        List<LocationAnnotation> locationEntities = CollectionHelper.newArrayList();
        Set<Location> anchorLocations = CollectionHelper.newHashSet();
        MultiMap<String, Location> locationMap = MultiMap.create();

        // try to find them in the database
        for (Annotated locationCandidate : taggedEntities) {

            String entityValue = locationCandidate.getValue();

            entityValue = PalladianLocationExtractor.cleanName(entityValue);

            // search entities by name
            // Collection<Location> retrievedLocations = locationSource.getLocations(entityValue,
            // EnumSet.of(Language.ENGLISH));
            Collection<Location> retrievedLocations = cache.get(entityValue);

            if (retrievedLocations == null) {
                continue;
            }

            // if we retrieved locations with AND without coordinates, only keep those WITH coordinates
            Filter<Location> coordFilter = new Filter<Location>() {
                @Override
                public boolean accept(Location item) {
                    return item.getLatitude() != null && item.getLongitude() != null;
                }
            };
            HashSet<Location> temp = CollectionHelper.filter(retrievedLocations, coordFilter, new HashSet<Location>());
            if (temp.size() > 0) {
                retrievedLocations = temp;
            }

            if (retrievedLocations.isEmpty()) {
                continue;
            }
            for (Location location : retrievedLocations) {
                if (EnumSet.of(LocationType.CONTINENT, LocationType.COUNTRY).contains(location.getType())) {
                    anchorLocations.add(location);
                }
                // XXX experimental : add places with high population count to
                // anchor locations. we should determine how to set a good threshold here.
                // improves recall/f1, slightly drops precision
                if (location.getPopulation() > 500000) {
                    LOGGER.debug("High prob location " + location);
                    anchorLocations.add(location);
                }

            }

            boolean ambiguous = checkAmbiguity(retrievedLocations);
            if (ambiguous) {
                LOGGER.debug("- " + entityValue + " is ambiguous!");
            } else {
                LOGGER.debug("+ " + entityValue + " is not amiguous: " + retrievedLocations);
            }

            if (!locationMap.containsKey(entityValue)) {
                locationMap.addAll(entityValue, retrievedLocations);
            }

            Location location = selectLocation(retrievedLocations);

            LocationAnnotation locationAnnotation = new LocationAnnotation(locationCandidate, location);
            locationEntities.add(locationAnnotation);
            // Location location = retrievedLocations.iterator().next();

            if (!ambiguous && entityValue.split("\\s").length >= 3) {
                LOGGER.debug("Adding {} to anchor locations, because of long name", location.getPrimaryName());
                anchorLocations.add(location);
            }
        }

        disambiguate(new HashSet<Location>(anchorLocations), locationMap);

        Set<Location> consolidatedLocations = CollectionHelper.newHashSet();
        consolidatedLocations.addAll(anchorLocations);
        for (List<Location> temp : locationMap.values()) {
            consolidatedLocations.addAll(temp);
        }

        Map<String, Location> finalResultsForCheck = CollectionHelper.newHashMap();

        Iterator<LocationAnnotation> iterator = locationEntities.iterator();
        Set<LocationAnnotation> toRemove = CollectionHelper.newHashSet();
        List<LocationAnnotation> toAdd = CollectionHelper.newArrayList();
        while (iterator.hasNext()) {
            LocationAnnotation annotation = iterator.next();
            String entityValue = annotation.getValue();

            entityValue = PalladianLocationExtractor.cleanName(entityValue);

            if (!locationMap.containsKey(entityValue)) {
                iterator.remove();
                continue;
            }
            if (locationMap.get(entityValue).size() == 0) {
                iterator.remove();
                continue;
            }
            if (locationMap.get(entityValue).size() > 1) {
                LOGGER.debug("Ambiguity for {}", entityValue);
            }
            Location loc = selectLocation(locationMap.get(entityValue));
            toRemove.add(annotation);

            toAdd.add(new LocationAnnotation(annotation, loc));

            finalResultsForCheck.put(annotation.getValue(), loc);
        }

        locationEntities.removeAll(toRemove);
        locationEntities.addAll(toAdd);

        Map<String, Location> clearMap = checkFinalResults(finalResultsForCheck, anchorLocations);
        iterator = locationEntities.iterator();
        while (iterator.hasNext()) {
            LocationAnnotation current = iterator.next();
            if (clearMap.containsKey(current.getValue())) {
                LOGGER.debug("- remove - " + current);
                iterator.remove();
            }
        }

        return locationEntities;

    }


    private void disambiguate(Set<Location> anchorLocations, MultiMap<String, Location> ambiguousLocations) {

        Set<Location> toAdd = CollectionHelper.newHashSet();
        for (Location location : anchorLocations) {
            List<Location> hierarchy = locationSource.getLocations(location.getAncestorIds());
            for (Location currentLocation : hierarchy) {
                if (currentLocation.getPrimaryName().equalsIgnoreCase("earth")) {
                    continue;
                }
                toAdd.add(currentLocation);
            }
        }
        anchorLocations.addAll(toAdd);

        // if we have countries as anchors, we remove the continents, to be more precise.
        LocationTypeFilter countryFilter = new LocationTypeFilter(LocationType.COUNTRY);
        if (CollectionHelper.filter(anchorLocations, countryFilter, new HashSet<Location>()).size() > 0) {
            CollectionHelper.filter(anchorLocations, countryFilter);
        }

        if (anchorLocations.size() == 0) {
            LOGGER.debug("No anchor locations");
            return;
        }

        LOGGER.debug("Anchor locations: {}", anchorLocations);

        // go through each group
        for (String locationName : ambiguousLocations.keySet()) {

            LOGGER.debug(locationName);

            List<Location> list = ambiguousLocations.get(locationName);

            // check each location in group
            Iterator<Location> it = list.iterator();
            while (it.hasNext()) {

                Location location = it.next();

                boolean anchored = false;
                List<Integer> hierarchyIds = location.getAncestorIds();

                // XXX experimental code; also keep locations without hierarchy
                if (hierarchyIds.isEmpty()) {
                    anchored = true;
                }

                for (Location anchorLocation : anchorLocations) {
                    if (hierarchyIds.contains(anchorLocation.getId())) {
                        anchored = true;
                    }
                }

                // trivial case
                if (anchorLocations.contains(location)) {
                    anchored = true;
                }

                if (location.getType() == LocationType.CONTINENT) {
                    anchored = true;
                }

                LOGGER.debug(anchored + " -> " + location);

                if (!anchored) {
                    it.remove();
                }

            }
            LOGGER.debug("-----------");
        }

    }

    /**
     * Check, if a Collection of {@link ImmutableLocation}s are "ambiguous". The condition of ambiguity is fulfilled, if
     * two
     * given Locations in the Collection have a greater distance then a specified threshold.
     * 
     * @param locations
     * @return
     */
    public static boolean checkAmbiguity(Collection<Location> locations) {
        if (locations.size() <= 1) {
            return false;
        }
        List<Location> temp = new ArrayList<Location>(locations);
        for (int i = 0; i < temp.size(); i++) {
            Location location1 = temp.get(i);
            for (int j = i + 1; j < temp.size(); j++) {
                Location location2 = temp.get(j);
                double distance = GeoUtils.getDistance(location1, location2);
                if (distance > 50) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Select one location when multiple were retrieved.
     * 
     * @param retrievedLocations
     * @return
     */
    private Location selectLocation(Collection<Location> retrievedLocations) {
        List<Location> temp = new ArrayList<Location>(retrievedLocations);
        Collections.sort(temp, new Comparator<Location>() {
            @Override
            public int compare(Location l1, Location l2) {
                if (l1.getType() != l2.getType()) {
                    if (l1.getType() == LocationType.CONTINENT) {
                        return -1;
                    }
                    if (l2.getType() == LocationType.CONTINENT) {
                        return 1;
                    }
                }
                Long l1Population = l1.getPopulation();
                Long l2Population = l2.getPopulation();
                if (l1.getType() == LocationType.CITY) {
                    l1Population *= 2;
                }
                if (l2.getType() == LocationType.CITY) {
                    l2Population *= 2;
                }
                return l2Population.compareTo(l1Population);
            }
        });
        return CollectionHelper.getFirst(temp);
    }

    private Map<String, Location> checkFinalResults(Map<String, Location> finalResultsForCheck,
            Set<Location> anchorLocations) {
        List<Entry<String, Location>> locationList = new ArrayList<Entry<String, Location>>(
                finalResultsForCheck.entrySet());
        Map<String, Location> toClear = CollectionHelper.newHashMap();
        for (int i = 0; i < locationList.size(); i++) {
            Location l1 = locationList.get(i).getValue();
            if (l1.getType() == LocationType.CONTINENT || l1.getType() == LocationType.COUNTRY
                    || l1.getType() == LocationType.REGION) {
                continue;
            }
            if (anchorLocations.contains(l1)) {
                continue; // always accepted.
            }
            double smallestDistance = Double.MAX_VALUE;
            Location smallestLoc = null;
            for (int j = 0; j < locationList.size(); j++) {
                Location l2 = locationList.get(j).getValue();
                if (l1.equals(l2)) {
                    continue;
                }
                double distance = GeoUtils.getDistance(l1, l2);
                if (smallestDistance > distance) {
                    smallestDistance = distance;
                    smallestLoc = l2;
                }
            }
            if (l1.getPopulation() == null || l1.getPopulation() < 5000) {
                LOGGER.debug(l1.getPrimaryName() + " : " + smallestDistance + " --- " + smallestLoc);
                if (smallestDistance > 250) {
                    toClear.put(locationList.get(i).getKey(), l1);
                }
            }
        }
        return toClear;
    }

}
