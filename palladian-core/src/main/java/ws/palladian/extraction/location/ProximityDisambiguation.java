package ws.palladian.extraction.location;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.PalladianLocationExtractor.LocationLookup;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.processing.features.Annotated;

public class ProximityDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProximityDisambiguation.class);

    private static final int DISTANCE_THRESHOLD = 150;

    @Override
    public List<LocationAnnotation> disambiguate(List<Annotated> annotations, LocationLookup cache) {
        List<LocationAnnotation> result = CollectionHelper.newArrayList();

        Collection<Location> anchorLocations = getAnchors(annotations, cache);

        for (Annotated annotation : annotations) {
            Collection<Location> locations = cache.get(annotation.getValue());
            if (locations.isEmpty()) {
                continue; // no match
            }

            Collection<Location> otherAnchors = new HashSet<Location>(anchorLocations);
            otherAnchors.removeAll(locations);

            Set<Location> selection = CollectionHelper.newHashSet();
            for (Location current : locations) {
                if (anchorLocations.contains(current)) {
                    selection.add(current);
                    continue;
                }
                for (Location other : otherAnchors) {
                    double distance = GeoUtils.getDistance(current, other);
                    if (distance < DISTANCE_THRESHOLD) {
                        selection.add(current);
                    }
                }
            }
            if (selection.size() > 0) {
                Location best = selectLocation(selection);
                result.add(new LocationAnnotation(annotation, best));
            }
        }
        return result;
    }

    // XXX copied from old code.
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

    public static Collection<Location> getAnchors(List<Annotated> annotations, LocationLookup cache) {
        Collection<Location> allLocations = cache.getAll();
        Collection<Location> anchorLocations = CollectionHelper.newHashSet();

        for (Annotated annotation : annotations) {
            Collection<Location> locations = cache.get(annotation.getValue());
            if (locations.isEmpty()) {
                continue;
            }
            boolean ambiguous = FirstDisambiguation.checkAmbiguity(locations);
            if (ambiguous) {
                continue;
            }
//            Location location = CollectionHelper.getFirst(locations);
//            if (location.getPopulation() > 5000) {
//                boolean added = anchorLocations.add(location);
//                if (added) {
//                    LOGGER.debug("Unambiguous achor location {}", location);
//                }
//            }
            for (Location location : locations) {
                if (location.getPopulation() != null && location.getPopulation() > 5000) {
                    LOGGER.debug("Unambiguous achor location {}", location);
                    anchorLocations.add(location);
                }
            }
        }

        // determine anchor locations
        for (Location location : allLocations) {
            LocationType type = location.getType();
            if (type == LocationType.CONTINENT || type == LocationType.COUNTRY || location.getPopulation() > 1000000) {
                boolean added = anchorLocations.add(location);
                if (added) {
                    LOGGER.info("Anchor location: {}", location);
                }
            }
        }

        // FIXME doesn't make (much) sense
        if (anchorLocations.isEmpty()) {
            Location biggestLocation = null;
            long maxPopulation = 0;
            for (Location location : allLocations) {
                if (location.getPopulation() > maxPopulation) {
                    maxPopulation = location.getPopulation();
                    biggestLocation = location;
                }
            }
            if (biggestLocation != null) {
                boolean added = anchorLocations.add(biggestLocation);
                if (added) {
                    LOGGER.info("Anchor location: {}", biggestLocation);
                }
            }
        }
        return anchorLocations;
    }

}
