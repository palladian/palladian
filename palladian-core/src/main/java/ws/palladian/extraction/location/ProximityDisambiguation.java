package ws.palladian.extraction.location;

import static ws.palladian.extraction.location.LocationType.CITY;
import static ws.palladian.extraction.location.LocationType.CONTINENT;
import static ws.palladian.extraction.location.LocationType.COUNTRY;
import static ws.palladian.extraction.location.LocationType.UNIT;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ws.palladian.extraction.location.LocationExtractorUtils.CoordinateFilter;
import ws.palladian.helper.collection.CollectionHelper;
import ws.palladian.helper.collection.MultiMap;
import ws.palladian.processing.features.Annotated;

public class ProximityDisambiguation implements LocationDisambiguation {

    /** The logger for this class. */
    private static final Logger LOGGER = LoggerFactory.getLogger(ProximityDisambiguation.class);

    private static final int DISTANCE_THRESHOLD = 150;

    @Override
    public List<LocationAnnotation> disambiguate(List<Annotated> annotations, MultiMap<String, Location> locations) {

        List<LocationAnnotation> result = CollectionHelper.newArrayList();

        Set<Location> anchors = getAnchors(locations);

        for (Annotated annotation : annotations) {
            String value = LocationExtractorUtils.normalizeName(annotation.getValue());
            Collection<Location> candidates = locations.get(value);
            if (candidates.isEmpty()) {
                LOGGER.debug("'{}' could not be found and will be dropped", annotation.getValue());
                continue;
            }

            LOGGER.debug("'{}' has {} candidates", annotation.getValue(), candidates.size());

            // for distance checks below, only consider anchor locations, which are not in the current candidate set
            Collection<Location> currentAnchors = new HashSet<Location>(anchors);
            currentAnchors.removeAll(candidates);

            Set<Location> preselection = CollectionHelper.newHashSet();

            for (Location candidate : candidates) {
                if (anchors.contains(candidate)) {
                    preselection.add(candidate);
                    continue;
                }
                for (Location anchor : currentAnchors) {
                    double distance = GeoUtils.getDistance(candidate, anchor);
                    LocationType anchorType = anchor.getType();
                    if (distance < DISTANCE_THRESHOLD) {
                        LOGGER.debug("Distance of {} to anchors: {}", distance, candidate);
                        preselection.add(candidate);
                    } else if (anchorType == CITY || anchorType == UNIT || anchorType == COUNTRY) {
                        if (LocationExtractorUtils.isChildOf(candidate, anchor) && candidate.getPopulation() > 5000) {
                            LOGGER.debug("{} is child of anchor '{}'", candidate, anchor.getPrimaryName());
                            preselection.add(candidate);
                        }
                    }
                }
            }
            if (preselection.size() > 0) {
                Location selection = selectLocation(preselection);
                result.add(new LocationAnnotation(annotation, selection));
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
                    if (l1.getType() == CONTINENT) {
                        return -1;
                    }
                    if (l2.getType() == CONTINENT) {
                        return 1;
                    }
                }
                Long l1Population = l1.getPopulation();
                Long l2Population = l2.getPopulation();
                if (l1.getType() == CITY) {
                    l1Population *= 2;
                }
                if (l2.getType() == CITY) {
                    l2Population *= 2;
                }
                return l2Population.compareTo(l1Population);
            }
        });
        return CollectionHelper.getFirst(temp);
    }

    private static Set<Location> getAnchors(MultiMap<String, Location> locations) {
        Set<Location> anchorLocations = CollectionHelper.newHashSet();

        // get prominent anchor locations; continents, countries and locations with very high population
        for (Location location : locations.allValues()) {
            LocationType type = location.getType();
            long population = location.getPopulation() != null ? location.getPopulation() : 0;
            if (type == CONTINENT || type == COUNTRY || population > 1000000) {
                LOGGER.debug("Prominent anchor location: {}", location);
                anchorLocations.add(location);
            }
        }

        // get unique and unambiguous locations; location whose name only occurs once, or which are very closely
        // together (because we might have multiple entries in the database with the same name which lie on a cluster)
        for (String name : locations.keySet()) {
            Collection<Location> group = locations.get(name);

            // in case we have locations with same name, but once with and without coordinates in the DB, we drop those
            // without coordinates
            group = LocationExtractorUtils.filterConditionally(group, new CoordinateFilter());

            if (LocationExtractorUtils.getLargestDistance(group) < 50) {
                for (Location location : group) {
                    long population = location.getPopulation() != null ? location.getPopulation() : 0;
                    if (population > 5000 || name.split("\\s").length > 2) {
                        LOGGER.debug("Unambiguous anchor location: {}", location);
                        anchorLocations.add(location);
                    }
                }
            } else {
                LOGGER.debug("Ambiguous location: {} ({} candidates)", name, group.size());
            }
        }

        // if we could not get any anchor locations, just take the biggest one from the given candidates
        if (anchorLocations.isEmpty()) {
            Location biggest = LocationExtractorUtils.getBiggest(locations.allValues());
            if (biggest != null) {
                LOGGER.debug("Biggest anchor location: {}", biggest);
                anchorLocations.add(biggest);
            }
        }
        return anchorLocations;
    }

}
